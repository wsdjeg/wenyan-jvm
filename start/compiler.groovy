try{
    Exception ans_1 = new Exception("滅頂")
    throw ans_1
}catch(ans_2){
    if(ans_2.message.equals("ReferenceError")){
        def ans_3 = ''+"本無此物。奈何用之"+''
        println(ans_3)
    }else if(ans_2.message.equals("SyntaxError")){
        def ans_4 = ''+"不合文法。不通之甚"+''
        println(ans_4)
    }else if(ans_2.message.equals("TypeError")){
        def ans_5 = ''+"物各其類。豈可混同"+''
        println(ans_5)
    }else if(ans_2.message.equals("滅頂")){
        def ans_6 = ''+"嗚呼哀哉。伏维尚飨"+''
        println(ans_6)
    }else{
        def ans_7 = ans_2
        def 奇禍 = ans_7

        def ans_8 = ''+"人坐家中。禍從天降"+''
        println(ans_8)
    }
}