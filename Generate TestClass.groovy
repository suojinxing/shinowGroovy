import com.intellij.database.model.DasTable
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

prefixLength = 1
packageName = ""
typeMapping = [
        (~/(?i)int/)                             : "Integer",
        (~/(?i)bool|bit/)                        : "Boolean",
        (~/(?i)float|double|decimal|real/)       : "Double",
        (~/(?i)date|time|datetime|timestamp/)    : "Date",
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

def getSetPropertyMethod(propertyName) {
    return (propertyName[0] + "").toUpperCase() + propertyName.substring(1, propertyName.length())
}

def generate(packageName, out, className, fields) {
    className = className.substring(prefixLength)

    out.println "package $packageName;"
    out.println ""
    out.println "import com.shinow.abc.common.domain.infrastructure.persistence.SessionManager;\n" +
            "import org.hibernate.HibernateException;\n" +
            "import org.hibernate.Session;\n" +
            "import org.hibernate.Transaction;\n" +
            "import org.junit.After;\n" +
            "import org.junit.Assert;\n" +
            "import org.junit.Before;\n" +
            "import org.junit.Test;"
    out.println "import static org.junit.Assert.assertNotNull;"

    // 标识是否有日期类型
    def flag = false;
    fields.each() {
        // 判断是否有日期类型，来导入日期类。
        if (it.type == "Date") {
            flag = true;
        }
    }
    if (flag) {
        out.println "import java.util.Date;"
    }
    out.println ""

    out.println "public class Hibernate${className}Test {"

    out.println "    @Before\n" +
            "    public void setUp() throws Exception {\n" +
            "    }\n" +
            "\n" +
            "    @After\n" +
            "    public void tearDown() throws Exception {\n" +
            "    }"

    out.println "    @Test\n" +
            "    public void crud() {"
    def entityName = "entity"
    def setProperty = ""
    def getProperty = ""

    // 生成实体类。new一个
    out.println "       " + className + " ${entityName} = new ${className}();"
//    out.println "       " + className + " ${entityName} = new ${className}();"
    fields.each() {
        // 对uuid不做操作
        if (it.name == "uuid") {
            return true;
        }

        // 判断类型，自动填值
        if (it.type == "Integer") {
            out.println "       entity.set" + getSetPropertyMethod(it.name) + "(100);"
        }
        if (it.type == "Double") {
            out.println "       entity.set" + getSetPropertyMethod(it.name) + "(100D);"
        }
        if (it.type == "Long") {
            out.println "       entity.set" + getSetPropertyMethod(it.name) + "(100L);"
        }
        if (it.type == "Date") {
            out.println "       entity.set" + getSetPropertyMethod(it.name) + "(new Date());"
        }
        if (it.type == "String") {
            out.println "       entity.set" + getSetPropertyMethod(it.name) + "(\"String\");"
            setProperty = "set" + getSetPropertyMethod(it.name) + "(\"str_hiahiahia\")"
            getProperty = "get" + getSetPropertyMethod(it.name) + "()"
        }


    }

    out.println "       Transaction tx = null;"
    // 第一个测试方法
    generateTryStart(out, "session")
    out.println "           session.save(${entityName});"
    generateTryEnd(out)

    // 第二个测试方法
    generateTryStart(out, "session1")
    out.println "           ${className} ${entityName}Read = (${className}) session1.get(${className}.class, ${entityName}.getUuid());"
    out.println "           assertNotNull(${entityName}Read);"
    generateTryEnd(out)

    // 第三个测试方法
    generateTryStart(out, "session2")
    out.println "           ${className} ${entityName}Read = (${className}) session2.get(${className}.class, ${entityName}.getUuid());"
    out.println "           assertNotNull(${entityName}Read);"
    out.println "           ${entityName}Read." + setProperty + ";"
    out.println "           session2.update(${entityName}Read);"
    generateTryEnd(out)

    // 第四个测试方法
    generateTryStart(out, "session3")
    out.println "           ${className} ${entityName}Read = (${className}) session3.get(${className}.class, ${entityName}.getUuid());"
    out.println "           assertNotNull(${entityName}Read);"
    out.println "           Assert.assertEquals(${entityName}Read." + getProperty + ",\"str_hiahiahia\");"
    generateTryEnd(out)

    // 第五个测试方法
    generateTryStart(out, "session4")
    out.println "           ${className} ${entityName}Read = (${className}) session4.get(${className}.class, ${entityName}.getUuid());"
    out.println "           assertNotNull(${entityName}Read);"
    out.println "           session4.delete(${entityName}Read);"
    generateTryEnd(out)


    out.println "       }"
    out.println "}"
}


def generateTryStart(out, sessionName) {
    out.println "       Session ${sessionName} = SessionManager.getInstance().getCurrentSession(\"please_replace_the_session_key.test\");"
    out.println "       try {"
    out.println "           tx = ${sessionName}.beginTransaction();"
}

def generateTryEnd(out) {
    out.println "           tx.commit();"
    out.println "       } catch (HibernateException e) {\n" +
            "           if (tx != null) {\n" +
            "               tx.rollback();\n" +
            "           }\n" +
            "           throw e;\n" +
            "       }"
    out.println ""
}

def generate(table, dir) {
    def className = javaName(table.getName(), true)
    def fields = calcFields(table)
    packageName = getPackageName(dir);
    new File(dir, "Hibernate" + className.substring(prefixLength) + "Test" + ".java").withPrintWriter { out -> generate(packageName, out, className, fields) }
//    new File(dir, "Hibernate" + className + "Test" + ".java").withPrintWriter { out -> generate(packageName, out, className, fields) }
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