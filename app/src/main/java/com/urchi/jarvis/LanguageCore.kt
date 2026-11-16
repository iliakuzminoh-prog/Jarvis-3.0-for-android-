package com.urchi.jarvis

import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow

/** Offline education/language core. Это не большая нейросеть: только проверяемые локальные алгоритмы и данные. */
object LanguageCore {
    data class Element(val number:Int,val symbol:String,val name:String,val mass:String)
    private val elements = listOf(
"1 H Водород 1.008","2 He Гелий 4.003","3 Li Литий 6.94","4 Be Бериллий 9.012","5 B Бор 10.81","6 C Углерод 12.011","7 N Азот 14.007","8 O Кислород 15.999","9 F Фтор 18.998","10 Ne Неон 20.180",
"11 Na Натрий 22.990","12 Mg Магний 24.305","13 Al Алюминий 26.982","14 Si Кремний 28.085","15 P Фосфор 30.974","16 S Сера 32.06","17 Cl Хлор 35.45","18 Ar Аргон 39.948","19 K Калий 39.098","20 Ca Кальций 40.078",
"21 Sc Скандий 44.956","22 Ti Титан 47.867","23 V Ванадий 50.942","24 Cr Хром 51.996","25 Mn Марганец 54.938","26 Fe Железо 55.845","27 Co Кобальт 58.933","28 Ni Никель 58.693","29 Cu Медь 63.546","30 Zn Цинк 65.38",
"31 Ga Галлий 69.723","32 Ge Германий 72.630","33 As Мышьяк 74.922","34 Se Селен 78.971","35 Br Бром 79.904","36 Kr Криптон 83.798","37 Rb Рубидий 85.468","38 Sr Стронций 87.62","39 Y Иттрий 88.906","40 Zr Цирконий 91.224",
"41 Nb Ниобий 92.906","42 Mo Молибден 95.95","43 Tc Технеций 98","44 Ru Рутений 101.07","45 Rh Родий 102.906","46 Pd Палладий 106.42","47 Ag Серебро 107.868","48 Cd Кадмий 112.414","49 In Индий 114.818","50 Sn Олово 118.710",
"51 Sb Сурьма 121.760","52 Te Теллур 127.60","53 I Йод 126.904","54 Xe Ксенон 131.293","55 Cs Цезий 132.905","56 Ba Барий 137.327","57 La Лантан 138.905","58 Ce Церий 140.116","59 Pr Празеодим 140.908","60 Nd Неодим 144.242",
"61 Pm Прометий 145","62 Sm Самарий 150.36","63 Eu Европий 151.964","64 Gd Гадолиний 157.25","65 Tb Тербий 158.925","66 Dy Диспрозий 162.500","67 Ho Гольмий 164.930","68 Er Эрбий 167.259","69 Tm Тулий 168.934","70 Yb Иттербий 173.045",
"71 Lu Лютеций 174.967","72 Hf Гафний 178.49","73 Ta Тантал 180.948","74 W Вольфрам 183.84","75 Re Рений 186.207","76 Os Осмий 190.23","77 Ir Иридий 192.217","78 Pt Платина 195.084","79 Au Золото 196.967","80 Hg Ртуть 200.592",
"81 Tl Таллий 204.38","82 Pb Свинец 207.2","83 Bi Висмут 208.980","84 Po Полоний 209","85 At Астат 210","86 Rn Радон 222","87 Fr Франций 223","88 Ra Радий 226","89 Ac Актиний 227","90 Th Торий 232.038",
"91 Pa Протактиний 231.036","92 U Уран 238.029","93 Np Нептуний 237","94 Pu Плутоний 244","95 Am Америций 243","96 Cm Кюрий 247","97 Bk Берклий 247","98 Cf Калифорний 251","99 Es Эйнштейний 252","100 Fm Фермий 257",
"101 Md Менделевий 258","102 No Нобелий 259","103 Lr Лоуренсий 266","104 Rf Резерфордий 267","105 Db Дубний 268","106 Sg Сиборгий 269","107 Bh Борий 270","108 Hs Хассий 277","109 Mt Мейтнерий 278","110 Ds Дармштадтий 281",
"111 Rg Рентгений 282","112 Cn Коперниций 285","113 Nh Нихоний 286","114 Fl Флеровий 289","115 Mc Московий 290","116 Lv Ливерморий 293","117 Ts Теннессин 294","118 Og Оганесон 294"
    ).map { r -> val p=r.split(" "); Element(p[0].toInt(),p[1],p.subList(2,p.size-1).joinToString(" "),p.last()) }

    fun answer(command:String):String? {
        if (command.length > MAX_INPUT_LENGTH) return null
        val q=norm(command)
        if(q.contains("таблиц")&&q.contains("мендел")) return "Таблица Менделеева содержит 118 официально известных элементов. Скажи «элемент кислород», «элемент Fe» или «элемент 26»."
        if(q.startsWith("элемент ")) return element(q.removePrefix("элемент ").trim())
        if(q.startsWith("атомный номер ")) return element(q.removePrefix("атомный номер ").trim())
        solveLinear(q)?.let { return it }
        solveMath(q)?.let { return it }
        return null
    }

    private fun element(q0:String):String {
        val q=norm(q0)
        val e=elements.firstOrNull{it.number.toString()==q||it.symbol.lowercase(Locale.ROOT)==q||it.name.lowercase(Locale.ROOT)==q}
            ?: return "Не нашёл элемент «" + q0 + "» в таблице Менделеева."
        return e.number.toString()+". "+e.name+" ("+e.symbol+"), относительная атомная масса ≈ "+e.mass+"."
    }

    private fun solveLinear(q:String):String? {
        val s=q.removePrefix("реши ").removePrefix("решить ").trim()
        if(!s.contains("=")||!s.contains("x")) return null
        val p=s.split("=",limit=2); if(p.size!=2) return null
        val l=coeff(p[0])?:return null; val r=coeff(p[1])?:return null
        val a=l.first-r.first; val b=r.second-l.second
        if(abs(a)<1e-10) return if(abs(b)<1e-10) "Уравнение имеет бесконечно много решений." else "Уравнение не имеет решений."
        return "Решение: x = "+fmt(b/a)+"."
    }

    private fun coeff(s:String):Pair<Double,Double>? {
        val v=s.replace(" ","").replace("−","-")
        if(v.isBlank()) return null
        var a=0.0; var b=0.0
        for(t0 in v.replace("-","+-").split("+").filter{it.isNotBlank()}){
            val t=t0.replace("*","")
            if(t.contains("x")){
                val c=t.removeSuffix("x")
                a+=when(c){"","+"->1.0;"-"->-1.0;else->c.toDoubleOrNull()?:return null}
            } else b+=t.toDoubleOrNull()?:return null
        }
        return a to b
    }

    private fun solveMath(q:String):String? {
        var e=q.replace("сколько будет","").replace("посчитай","").replace("вычисли","").replace("реши","")
            .replace("равно","").replace(",",".").replace("х","*").replace("×","*").replace("−","-").trim()
        if(e.isBlank()||!Regex("^[0-9+\\-*/().^%\\s]+$").matches(e)) return null
        if(!Regex(".*[+*/^%].*").matches(e)&&!Regex(".*\\d+\\s*-\\s*\\d+.*").matches(e)) return null
        return try{"Ответ: "+fmt(Parser(e).parse())+"."}catch(_:Exception){null}
    }
    private fun fmt(v:Double)=if(abs(v-v.toLong())<1e-10)v.toLong().toString() else String.format(Locale.US,"%.8f",v).trimEnd('0').trimEnd('.')
    private fun norm(s:String)=s.lowercase(Locale.ROOT).replace('ё','е')
        .replace(Regex("[^\\p{L}\\p{N} ]"), " ")
        .replace(Regex("\\s+")," ").trim()

    private class Parser(private val s:String){
        var p=0
        fun parse():Double{val v=expr();skip();if(p!=s.length)error("syntax");return v}
        private fun expr():Double{var v=term();while(true){skip();v=if(take('+'))v+term() else if(take('-'))v-term() else return v}}
        private fun term():Double{var v=power();while(true){skip();v=if(take('*'))v*power() else if(take('/'))v/power() else if(take('%'))v%power() else return v}}
        private fun power():Double{var v=unary();skip();if(take('^'))v=v.pow(power());return v}
        private fun unary():Double{skip();if(take('+'))return unary();if(take('-'))return -unary();if(take('(')){val v=expr();if(!take(')'))error(")");return v};val st=p;while(p<s.length&&(s[p].isDigit()||s[p]=='.'))p++;if(st==p)error("number");return s.substring(st,p).toDouble()}
        private fun take(c:Char):Boolean{skip();if(p<s.length&&s[p]==c){p++;return true};return false}
        private fun skip(){while(p<s.length&&s[p].isWhitespace())p++}
    }
    fun elementCount()=elements.size
    private const val MAX_INPUT_LENGTH = 256
}
