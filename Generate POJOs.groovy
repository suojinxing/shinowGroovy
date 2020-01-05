import com.intellij.database.model.DasTable
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

/*
 * Available context bindings:
 *   SELECTION   Iterable<DasObject>
 *   PROJECT     project
 *   FILES       files helper
 */

packageName = ""
typeMapping = [
        (~/(?i)int/)                             : "Integer",
        (~/(?i)bool|bit/)                        : "Boolean",
        (~/(?i)float|double|decimal|real/)       : "Double",
//        (~/(?i)datetime|timestamp/)              : "Long",
        (~/(?i)date|time|datetime|timestamp/)    : "Date",
        //(~/(?i)time/)                       : "java.sql.Time",
        (~/(?i)/)                                : "String",
        (~/(?i)blob|binary|bfile|clob|raw|image/): "InputStream"
]

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
    SELECTION.filter { it instanceof DasTable }.each { generate(it, dir) }
}

def getPackageName(dir) {
    def dirStr = dir.toString().replaceAll("\\\\", "/")
    dirStr = dirStr.split("/");
    def resultPackageName = "";
    def index = dirStr.length + 1;
    for (int i = 0; i < dirStr.length; i++) {
        if (dirStr[i] == "com") {
            index = i;
        }
        if (i >= index) {
            resultPackageName += dirStr[i] + ".";
        }
    }
    if (index == dirStr.length + 1) {
        return "";
    }
    return resultPackageName.substring(0, resultPackageName.length() - 1);
}

def generate(table, dir) {
    def className = javaName(table.getName(), true)
    def fields = calcFields(table)
    packageName = getPackageName(dir);
    new File(dir, className + ".java").withPrintWriter { out -> generate(packageName, out, className, fields) }
}

def generate(packageName, out, className, fields) {
    out.println "package $packageName;"
    out.println ""
    out.println "import com.shinow.abc.common.domain.Entity;"
    def flag = false;
    fields.each(){
        // 判断是否有日期类型，来导入日期类。
        if(it.type == "Date"){
            flag = true;
        }
    }
    if(flag){
        out.println "import java.util.Date;"
    }
    out.println ""
    out.println "public class $className extends Entity {"
    fields.each() {
        if (it.name == "uuid") {
            return true;
        }

        if (it.annos != "") out.println "  ${it.annos}"
        out.println "  private ${it.type} ${it.name};"
    }
    out.println ""
    fields.each() {
        if (it.name == "uuid") {
            return true;
        }
        if (it.type == "Date") {
            out.println "  public ${it.type} get${it.name.capitalize()}() {"
            out.println "       if (${it.name} == null) {"
            out.println "           return null;"
            out.println "       } else {"
            out.println "           return new Date(this.${it.name}.getTime());"
            out.println "       }"
            out.println "  }"
            out.println ""
            out.println "  public void set${it.name.capitalize()}(${it.type} ${it.name}) {"
            out.println "       if(${it.name} == null) {"
            out.println "           return ;"
            out.println "       }"
            out.println "       this.${it.name} = ${it.name};"
            out.println "  }"
            out.println ""
            return true;
        }
        out.println "  public ${it.type} get${it.name.capitalize()}() {"
        out.println "    return ${it.name};"
        out.println "  }"
        out.println ""
        out.println "  public void set${it.name.capitalize()}(${it.type} ${it.name}) {"
        out.println "    this.${it.name} = ${it.name};"
        out.println "  }"
        out.println ""
    }
    out.println "}"
}

def calcFields(table) {
    DasUtil.getColumns(table).reduce([]) { fields, col ->
        def spec = Case.LOWER.apply(col.getDataType().getSpecification())
        def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
        fields += [[
                           name : javaName(col.getName(), false),
                           type : typeStr,
                           annos: ""]]
    }
}

def javaName(str, capitalize) {
    def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
            .collect { Case.LOWER.apply(it).capitalize() }
            .join("")
            .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
    capitalize || s.length() == 1 ? s : Case.LOWER.apply(s[0]) + s[1..-1]
}