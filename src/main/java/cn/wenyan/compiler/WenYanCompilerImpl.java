package cn.wenyan.compiler;



import cn.wenyan.compiler.command.CommandHandler;
import cn.wenyan.compiler.command.CompilerConfig;
import cn.wenyan.compiler.factory.CompileFactory;
import cn.wenyan.compiler.factory.StreamBuilder;
import cn.wenyan.compiler.log.LogFormat;
import cn.wenyan.compiler.log.ServerLogger;
import cn.wenyan.compiler.plugins.Listener;
import cn.wenyan.compiler.plugins.PluginManager;
import cn.wenyan.compiler.script.libs.Language;
import cn.wenyan.compiler.script.libs.Library;
import cn.wenyan.compiler.script.libs.Syntax;
import cn.wenyan.compiler.streams.*;
import cn.wenyan.compiler.utils.*;
import org.apache.commons.io.FileUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.tools.Compiler;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static cn.wenyan.compiler.log.LogFormat.fg;


/**
 * 若施文言之术，必先用其器。古人云: 君子生非异，善假于物。
 * 此器以爪哇之法，行虚拟机之道，亦可广传文言于天下者。
 * 君用[run]之洋文可走之。吾欲将其译为java者也。
 *
 * @author 卢昶存 @ noyark.net
 */
public class WenYanCompilerImpl implements WenYanCompiler,Cloneable{

    private Map<File,File> wygFiles = new HashMap<>();

    private PrettyCode pretty;

    private boolean strongType = false;

    private boolean lexerViewer;

    protected Library library;

    private String compilingFile = "";

    private CompilerConfig compilerConfig;

    private List<Listener> listeners;

    private Map<String,String> nameType = new HashMap<>();

    private int indexCode;

    private boolean supportPinyin;

    private List<Integer> nowCompiling = new ArrayList<>();

    private LanguageCompiler groovyCompiler;

    private ServerLogger serverLogger;

    private CompileFactory factory;


    private List<String> wenyans;

    private CommandHandler handler;

    private Language languageType;

    private PluginManager pluginManager;

    private WenYanRuntime runtime;

    private Map<Class<? extends CompileStream>,CompileStream> streamMap;

    private String sourcePath;

    private String classPath;

    private List<String> compiled;

    private PrepareCompiler prepareCompiler;

    private String mainClass;

    //***************************************************//
    //*********************构造器*************************//
    //**************此为天地之造物者，乃于此乎。**************//
    //**************************************************//

    WenYanCompilerImpl(boolean supportPinyin,Language language){
        this(supportPinyin,language,null);
    }

    WenYanCompilerImpl(boolean supportPinyin, Language language, WenYanShell shell){
        this.languageType = language;
        this.library = new Library(language);
        this.streamMap = new HashMap<>();
        this.groovyCompiler = language.languageCompiler();
        this.serverLogger = new ServerLogger(new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().getFile()).getParentFile());
        this.handler = new CommandHandler(this);
        this.supportPinyin = supportPinyin;
        if(File.separator.equals("\\")) AnsiConsole.systemInstall();
        this.listeners = new ArrayList<>();
        this.factory = new StreamBuilder(this)
                .put(new VariableCompileStream(this))
                .put(new CommentCompileStream(this))
                .put(new ControlCompileStream(this))
                .put(new MathCompileStream(this))
                .put(new FunctionCompileStream(this))
                .put(new ArrayCompileStream(this))
                .put(new TryCompileStream(this))
                .put(new ObjectCompileStream(this))
                .build();
        this.pluginManager = new PluginManager(this);
        this.runtime = new WenYanRuntime(this,shell==null?new WenYanShell(this):shell);
        this.compiled = new ArrayList<>();
        this.mainClass = "";
        this.sourcePath = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().getFile()).getParent();
        this.classPath = sourcePath;
        this.loadPlugins();
        this.prepareCompiler = new PrepareCompiler(this);
        this.pretty = language.getPretty();
    }



    //***************************************************//
    //*********************编译主方法**********************//
    //**************************************************//

    @Override
    public int compile(String... args) {
        return handler.executeCommand(args);
    }


    //***************************************************//
    //***********************PUBLIC**********************//
    //**************************************************//

    public String dispatch(String wenyan){
        return compile(wenyan,true);
    }

    public Class<?> compileToClass(String className,String... wenyanString){
        Class<?> clz = groovyCompiler.compile(getGroovyCode(true,false,wenyanString),className);

        this.serverLogger.info("得类为:"+clz.getName());
        return clz;
    }

    public Class<?> compileToClass(String... wenyanString){
        return groovyCompiler.compile(getGroovyCode(true,false,wenyanString));
    }

    public WenYanRuntime getRuntime() {
        return runtime;
    }

    //---------------不建议作为API使用-----------------------//

    //***************************************************//
    //***********************内部调用*********************//
    //**************************************************//


    public int init(CompilerConfig compilerConfig){
        try {
            this.sourcePath = compilerConfig.getSourcePath();
            this.mainClass = compilerConfig.getMainClass();
            this.compilerConfig = compilerConfig;
            this.supportPinyin = compilerConfig.isSupportPinYin();
            this.lexerViewer = compilerConfig.isLexerViewer();
            this.strongType = compilerConfig.isStrongType();
            String[] files = compilerConfig.getCompileFiles();
            String[] args = compilerConfig.getRunArgs();
            String[] libs = compilerConfig.getCompileLib();
            String classFile = compilerConfig.getClassFile();
            String wenyuangeFile = compilerConfig.getWenyuangeFile();
            String wygDownload = compilerConfig.getWygDownload();
            boolean isRun = compilerConfig.isRun();
            classPath = compilerConfig.getOutFile();
            if(classPath == null)return 1;
            File cp = new File(classPath);
            if(wygDownload!=null){
                Runtime.getRuntime().exec("npm i -g @wenyanlang/wyg");
                Runtime.getRuntime().exec("wyg i "+wygDownload);
            }
            if(wenyuangeFile!=null){
                ///藏书楼的位置
                String wygf = wenyuangeFile+"/";
                File file = new File(wygf.equals("null/")?WYG:wygf+WYG);
                File[] fileLibs = file.listFiles();
                compileWygs(fileLibs,cp);
            }

            if ((classPath == null || files == null)&&!isRun) {
                serverLogger.info("必要: 输出文件路径和编译文件信息");
                return 1;
            }
            if(sourcePath == null&&!isRun){
                serverLogger.info("请指定sourcePath");
            }
            if(!isRun){
                this.serverLogger.info(LogFormat.textFormat(LogFormat.Control.BOLD.getAnsi()+"WenYan Lang JVM Compiler"+ fg(Ansi.Color.DEFAULT),Ansi.Color.YELLOW));
                this.serverLogger.info("@CopyRight wy-lang.org || github: https://github.com/LingDong-/wenyan-lang");
                this.serverLogger.info("WenYan 3rd Party Compiler : github: https://github.com/MagicLu550/wenyan-lang_jvm/blob/master/README.md");
                this.serverLogger.info("文言文语言的语法规则最终由LingDong的wenyan-lang为基本要素");
            }

            if(libs!=null){
                for(String lib : libs){
                    if(lib.endsWith(".jar")){
                        runtime.getShell().getRun().getClassLoader().addURL(new File(lib).toURI().toURL());
                    }
                }
            }

            if(files!=null) {
                for (String file : files) {
                    compileOut(sourcePath, new File(file), cp, mainClass, compilerConfig.isGroovy());
                }
            }
            // -jar a.jar;b.jar -o classPath -n className -r args
            if(isRun){

                if(classFile!=null){
                    try {
                        runtime.getShell().getRun().getClassLoader().addURL(new File(classPath).toURI().toURL());
                        Class<?> clz = runtime.getShell().getRun().getClassLoader().loadClass(classFile.replace(classPath,"").replace(File.separator,"."));
                        clz.getDeclaredMethod("main",String[].class).invoke(null,(Object)args);
                    }catch (Exception e){
                        serverLogger.error("",e);
                    }
                }
            }
            wygFiles.forEach((x,y)->x.renameTo(y));
        }catch (IOException e){
            serverLogger.error("",e);
        }
        return 0;
    }


    public List<String> compileToList(String wenyan,boolean outError){
        List<String> results = new ArrayList<>();
        try {
            wenyans = prepareCompiler.macroPrepare(wenyan);
            if(lexerViewer) serverLogger.info(LexerUtils.getLine(wenyans));
            while (wenyans.size() != 0) {
                results.add(factory.compile(0, wenyans).get(0));
            }
            return results;
        }catch (Exception e){
            String message = LogFormat.textFormat("[Syntax Error] " + e.getMessage(), Ansi.Color.RED) + fg(Ansi.Color.DEFAULT);
            if(outError) {
                this.serverLogger.error(message, e);
            }else{
                this.serverLogger.error(message);
            }
            e.printStackTrace();
            results.add(0,ERROR);
            return results;
        }
    }

    public String compile(String wenyan,boolean outError){
        List<String> results = compileToList(wenyan,outError);
        if(results.size()!=0) {
            if (results.get(0).equals(ERROR)) {
                return ERROR;
            }
        }
        getStream(FunctionCompileStream.class).toGlobal();

        return codesTocode(results,null);
    }


    public ServerLogger getServerLogger() {
        return serverLogger;
    }

    public void compileToClass(File out,File classFile) throws FileNotFoundException {
        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        PrintWriter writer = new PrintWriter(classFile);
        compilerConfiguration.setOutput(writer);
        compilerConfiguration.setTargetBytecode(CompilerConfiguration.JDK8);
        compilerConfiguration.setTargetDirectory(classPath);
        compilerConfiguration.setClasspath(classPath);
        Compiler compiler = new Compiler(compilerConfiguration);
        compiler.compile(out);
        writer.close();
    }

    public Map<Class<? extends CompileStream>, CompileStream> getStreamMap() {
        return streamMap;
    }

    public List<Integer> getNowCompiling() {
        return nowCompiling;
    }

    public boolean isSupportPinyin() {
        return supportPinyin;
    }

    public <T extends CompileStream> T getStream(Class<T> stream){
        return stream.cast(streamMap.get(stream));
    }

    public int getIndexCode() {
        return indexCode;
    }

    public void clearIndexCode(){
        indexCode = 0;
    }

    public void setIndexCode() {
        indexCode++;
    }

    public CompileFactory getFactory() {
        return factory;
    }

    public String removeWenyan(){
        setIndexCode();
        return this.wenyans.remove(0);
    }


    public Map<String, String> getNameType() {
        return nameType;
    }

    public Language getLanguageType() {
        return languageType;
    }

    public List<Listener> getListeners() {
        return listeners;
    }

    public void callListener(CompileStream stream,List<String> wenyans,boolean start){
        if(start){
            for(Listener listener : listeners){
                listener.onCompileStart(stream,wenyans);
            }
        }else{
            for(Listener listener : listeners){
                listener.onCompileFinish(stream,wenyans);
            }
        }
    }

    public String getCompilingFile() {
        return compilingFile;
    }

    public String getGroovyCode(boolean addNow, boolean outInConsole, String... wenyanString){
        StringBuilder groovyCode = new StringBuilder();
        if(addNow)
            groovyCode.append(languageType.getSyntax(Syntax.IMPORT_WITH));
        for(String code:wenyanString){
            String compile = compile(code,true);
            if(outInConsole){
                serverLogger.info(code+" => "+ compile);
            }
            groovyCode.append(compile).append("\n");
        }
        this.serverLogger.info("此事成也，得之");
        System.out.println("----------------------------WenYanConsole--------------------------------");
        indexCode = 0;
        return groovyCode.toString();
    }

    public String getWenYanCodeByFile(File wenyan) throws IOException{
        List<String> list = FileUtils.readLines(wenyan,System.getProperty("file.coding"));
        StringBuilder builder = new StringBuilder();
        for(String str:list){
            builder.append(str).append("\n");
        }
        return builder.toString();
    }



    public void compileOut(String classPath,File file, File outDir,String mainClass,boolean groovy) throws IOException{
        this.compilingFile = file.getName().split("\\.")[0];
        compileToGroovy(file,classPath,outDir, getWenYanCodeByFile(file),mainClass,groovy);
    }
    public String getSourcePath() {
        return sourcePath;
    }

    public String getClassPath() {
        return classPath;
    }

    public String getMainClass() {
        return mainClass;
    }

    public Library getLibrary() {
        return library;
    }

    public boolean isStrongType() {
        return strongType;
    }

    @Override
    public WenYanCompilerImpl clone() throws CloneNotSupportedException {
        return (WenYanCompilerImpl) super.clone();
    }


    //***************************************************//
    //***********************類内部调用*********************//
    //**************************************************//


    private String codesTocode(List<String> results,String filter){
        StringBuilder builder = new StringBuilder();
        for(String r :results) {
            if(r!=null&&filter!=null&&r.startsWith(filter))continue;
            builder.append("\n").append(r);
        }
        return builder.toString();
    }

    private List<String> makeStaticTocode(List<String> results,boolean get){
        if(get) {
            int index = 0;
            for (int i = 0; i < results.size(); i++) {
                if (index == 0) {
                    String result = results.get(i);
                    if (result.startsWith("def")||result.startsWith("class")){
                        StringBuilder builder = new StringBuilder();
                        String[] str = GroovyUtils.splitGroovyCode(result,"\n").toArray(new String[0]);
                        for(int z = 0;z<str.length;z++){
                            if(str[z].startsWith("def"))
                                builder.append("static ").append(str[z]).append("\n");
                            else
                                builder.append(str[z]).append("\n");
                        }
                        results.set(i,builder.toString());
                    }
                }
                index += Utils.getClose(results.get(i));
            }
        }
        return results;
    }


    private String getImports(List<String> results){
        StringBuilder builder = new StringBuilder();
        for(String r :results) {
            if(r!=null&&!r.startsWith("import"))continue;
            builder.append("\n").append(r);
        }
        return builder.toString();
    }

    private String getClassName(File thisFile,String sc){
        String className = thisFile.toString().replace(sc,"");
        if(className.startsWith(File.separator)){
            className = className.substring(1);
        }
        className = className.replace(File.separator,".");
        return className.substring(0,className.lastIndexOf("."));
    }

    private ResultEntry getStaticCode(List<String> results,boolean isMain){
        if(isMain){
            return new ResultEntry(results,"");
        }
        StringBuilder builder = new StringBuilder();
        List<String> rs = new ArrayList<>(results);
        int index = 0;
        for(int i = 0;i<results.size();i++){
            int close = Utils.getClose(results.get(i));
            index += close;
            if(index == 0){
                if(close == 0){
                    if(!results.get(i).startsWith("static")&&!results.get(i).startsWith("import")) {
                        builder.append(results.get(i)).append("\n");
                        rs.set(i, null);
                    } else if(results.get(i).startsWith("static")&&results.get(i).contains("=")){
                        String[] defs = GroovyUtils.splitGroovyCode(results.get(i),"\n").toArray(new String[0]);
                        StringBuilder builder1 = new StringBuilder();
                        for(String def : defs){
                            String[] name_value = GroovyUtils.splitGroovyCode(def,"=").toArray(new String[0]);
                            builder1.append(name_value[0]).append("\n");
                            builder.append(name_value[0].split(" ")[2]).append("=").append(name_value[1]).append("\n");
                        }
                        rs.set(i,builder1.toString());
                    }
                }
            }
        }
        Iterator<String> iterator = rs.iterator();
        while (iterator.hasNext()){
            if(iterator.next() == null)iterator.remove();
        }
        return new ResultEntry(rs,builder.toString());
    }

    //TODO 如果未来实现了类，必须要把类剥离出来
    //TODO groovy独特

    private File compileToGroovy(File thisFile,String sc,File file,String wenyanString,String mainClass,boolean isGroovy){
        try {
            StringBuilder builder = new StringBuilder();
            mainClass = mainClass==null?"":mainClass;
            String className = getClassName(thisFile,sc);
            int index = className.lastIndexOf(".");
            String pack = getPackage(className,index);
            File parent = new File(file+File.separator+pack.replace(".",File.separator));
            if(!parent.exists())parent.mkdirs();
            String name = File.separator+thisFile.getName().split("\\.")[0];
            File out = new File(parent,name+".groovy");
            File classFile = new File(parent,name+".class");
            if(!compiled.contains(classFile.toString())){
                compiled.add(classFile.toString());
            }else {
                return out;
            }

            appendClassName(index,mainClass,className,compileToList(wenyanString,false),builder,prepareCompiler.toAnnotation(wenyanString),pack);

            createOutFile(classFile,out,builder.toString(),parent,isGroovy);

            serverLogger.info("得文件为: "+out);
            return out;
        }catch (Exception e){
            serverLogger.error("Syntax Error",e);
            return null;
        }
    }

    private void appendClassName(int index,String mainClass,String className,List<String> codes,StringBuilder builder,String annotation,String pack){
        if(index !=-1){
            builder.append("package ");
            builder.append(pack);
        }
        boolean isMain = mainClass.equals(className);
        String filter = "import";
        ResultEntry codeEntry = getStaticCode(makeStaticTocode(codes,!isMain),isMain);
        String code = codesTocode(codeEntry.getCode(),filter);
        String imports = getImports(codes);

        builder.append("\n");
        builder.append(languageType.getSyntax(Syntax.IMPORT_WITH));
        builder.append("\n");
        builder.append(imports);
        if(!isMain){

            builder.append("\n");
            builder.append(annotation);
            builder.append("class ");
            builder.append(className.replace(pack,"").replace(".",""));
            builder.append("{");
            builder.append("\n");
        }
        builder.append(code);
        if(!isMain) {
            builder.append("\nstatic{\n");
            builder.append(codeEntry.getOutCode());
            builder.append("}");
            builder.append("\n}");
        }
    }

    private String getPackage(String className,int index){
        return index!=-1?className.substring(0,index):"";
    }

    private void createOutFile(File classFile,File out,String code,File parent,boolean isGroovy) throws IOException{
        if(!classFile.exists())classFile.createNewFile();

        FileUtils.write(out,pretty.pretty(code),System.getProperty("file.coding"));
        if(!parent.exists())parent.mkdirs();

        compileToClass(out,classFile);

        if(isGroovy)out.delete();
    }

    private void compileWyg(File f,File cp) throws IOException{
        File xu = new File(f,WYG_LIB);
        File xuAfter = new File(f,f.getName()+".wy");
        xu.renameTo(xuAfter);
        compileOut(sourcePath,xuAfter,cp,mainClass,compilerConfig.isGroovy());
        library.addLib(f.getName(),getClassName(xuAfter,sourcePath));
        wygFiles.put(xuAfter,xu);
    }

    private void compileWygs(File[] fileLibs,File cp) throws IOException{
        if(fileLibs!=null){
            for(File f : fileLibs){
                File[] fs = f.listFiles();
                if(fs!=null&&fs.length != 1){
                    compileWygs(fs, cp);
                }
                if(f.isDirectory()){
                    compileWyg(f,cp);
                }
            }
        }
    }

    private String trimWenYan(String s){
        return LexerUtils.trimWenYanX(s);
    }

    private void loadPlugins(){
        File pluginFile = new File(new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().getFile()).getParentFile()+"/plugins");
        if(!pluginFile.exists()) pluginFile.mkdirs();
        File[] plugins = pluginFile.listFiles();
        if(plugins != null){
            for(File f : plugins){
                if (f.toString().endsWith(".jar"))
                    pluginManager.loadPlugin(f);
            }
        }
    }


}
