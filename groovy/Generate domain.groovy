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

def generate(table, dir) {
    def className = javaName(table.getName(), true)
    def fields = calcFields(table)
    packageName = getPackageName(dir);
    // 实体类
    new File(dir, className + ".java").withPrintWriter { out -> generatePojo(packageName, out, className, fields) }
    // Service接口
    new File(dir, className + "QueryService.java").withPrintWriter { out -> generateService(packageName, out, className, fields) }
    // Service实现类
    new File(dir, "Hibernate" + className + "QueryService.java").withPrintWriter { out -> generateServiceImpl(packageName, out, className, fields) }
    // Repository接口
    new File(dir, className + "Repository.java").withPrintWriter { out -> generateRepository(packageName, out, className, fields) }
    // Repository实现类
    new File(dir, "Hibernate" + className + "Repository.java").withPrintWriter { out -> generateRepositoryImpl(packageName, out, className, fields) }
    // Hbm.xml
    new File(dir, className + ".hbm.xml").withPrintWriter { out -> generateHbmXml(out, className, fields, table) }
    // 生成测试类
    new File(dir, "Hibernate" + className + "Test" + ".java").withPrintWriter { out -> generateTestClass(packageName, out, className, fields) }
}
// 测试类
def static generateTestClass(packageName, out, className, fields) {
//    className = className.substring(prefixLength)

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

def static getSetPropertyMethod(propertyName) {
    return (propertyName[0] + "").toUpperCase() + propertyName.substring(1, propertyName.length())
}

def static generateTryStart(out, sessionName) {
    out.println "       Session ${sessionName} = SessionManager.getInstance().getCurrentSession(\"please_replace_the_session_key.test\");"
    out.println "       try {"
    out.println "           tx = ${sessionName}.beginTransaction();"
}

def static generateTryEnd(out) {
    out.println "           tx.commit();"
    out.println "       } catch (HibernateException e) {\n" +
            "           if (tx != null) {\n" +
            "               tx.rollback();\n" +
            "           }\n" +
            "           throw e;\n" +
            "       }"
    out.println ""
}

// 生成hbmxml文件
def static generateHbmXml(out, className, fields, table) {
    out.println "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
    out.println "<!DOCTYPE hibernate-mapping PUBLIC \"-//Hibernate/Hibernate Mapping DTD 3.0//EN\""
    out.println "\"http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd\">"
    out.println "<hibernate-mapping package=\"com.shinow.abc..domain.model\">"
    out.println "   <class name=\"\" table=\"" + table.getName() + "\""
    out.println "       dynamic-insert=\"true\" dynamic-update=\"true\">"
    out.println "       <id name=\"uuid\" type=\"string\" column=\"UUID\">"
    out.println "           <generator class=\"uuid\"/>"
    out.println "       </id>"
    fields.each() {
        if(it.name == "uuid"){
            return true;
        }
        out.println "      <property name=\"${it.name}\" type=\"" + getColumnType(it.type) + "\" column=\"${it.colName}\"/> "
    }
    out.println "   </class>"
    out.println "</hibernate-mapping>"
}

def static getColumnType(type) {
    if (type == "String") {
        return "string"
    }
    if (type == "Double") {
        return "double"
    }
    if (type == "Integer") {
        return "integer"
    }
    if (type == "Date") {
        return "timestamp"
    }
    return "Do not define the type,please input..."
}

// service
def static generateService(packageName, out, className, fields) {
    out.println "package $packageName;"
    out.println ""
    out.println "import com.shinow.abc.common.domain.infrastructure.PagedList;"
    out.println ""
    out.println "import java.text.ParseException;\n" +
            "import java.util.HashMap;\n" +
            "import java.util.List;"

    out.println "public interface ${className}QueryService {"
    out.println "    PagedList<${className}> find${className}s(int start, int limit, List<HashMap<String, String>> filters,\n" +
            "                                                          List<HashMap<String, String>> sorts) throws ParseException;"
    out.println "}"
}

def static generateServiceImpl(packageName, out, className, fields) {
    out.println "package $packageName;"
    out.println "import com.shinow.abc.common.domain.infrastructure.PagedList;\n" +
            "import com.shinow.abc.common.domain.port.adapter.persistence.hibernate.AbstractHibernateSession;"
    out.println "import org.hibernate.Criteria;\n" +
            "import org.hibernate.Session;\n" +
            "import org.hibernate.criterion.CriteriaSpecification;\n" +
            "import org.hibernate.criterion.Order;\n" +
            "import org.hibernate.criterion.Projections;\n" +
            "import org.hibernate.criterion.Restrictions;"
    out.println ""
    out.println "import java.text.DateFormat;\n" +
            "import java.text.ParseException;\n" +
            "import java.text.SimpleDateFormat;\n" +
            "import java.util.Date;\n" +
            "import java.util.HashMap;\n" +
            "import java.util.List;"
    out.println ""
    out.println "public class Hibernate${className}QueryService extends AbstractHibernateSession implements ${className}QueryService {\n" +
            "    public Hibernate${className}QueryService(Session session) {\n" +
            "        super(session);\n" +
            "    }"
    out.println "    @Override\n" +
            "    public PagedList<${className}> find${className}s(int start, int limit, List<HashMap<String, String>> filters, List<HashMap<String, String>> sorts) throws ParseException {"
    out.println "    PagedList<${className}> pagedList = new PagedList<>();\n" +
            "        Criteria criteria = this.session.createCriteria(${className}.class);\n" +
            "\n" +
            "        DateFormat df = new SimpleDateFormat(\"yyyy-MM-dd HH:mm:ss\");\n" +
            "        for (HashMap<String, String> filter : filters) {\n" +
            "            switch (filter.get(\"property\")) {\n" +
            "                case \"startDate\":\n" +
            "                    String date = filter.get(\"value\").substring(0, 10) + \" 00:00:00\";\n" +
            "                    Date starttime = df.parse(date);\n" +
            "                    criteria.add(Restrictions.ge(\"startTime\", starttime));\n" +
            "                    break;\n" +
            "                case \"endDate\":\n" +
            "                    String date1 = filter.get(\"value\").substring(0, 10) + \" 23:59:59\";\n" +
            "                    Date endtime = df.parse(date1);\n" +
            "                    criteria.add(Restrictions.lt(\"startTime\", endtime));\n" +
            "                    break;\n" +
            "            }\n" +
            "        }\n" +
            "\n" +
            "        long total = (Long) criteria.setProjection(Projections.rowCount()).uniqueResult();\n" +
            "        criteria.setProjection(null);\n" +
            "        criteria.setResultTransformer(CriteriaSpecification.ROOT_ENTITY);\n" +
            "        if (start >= total) {\n" +
            "            start -= limit;\n" +
            "        }\n" +
            "        criteria.setFirstResult(start);\n" +
            "        criteria.setMaxResults(limit);\n" +
            "        for (HashMap<String, String> sort : sorts) {\n" +
            "            if (sort.get(\"direction\").equals(\"ASC\")) {\n" +
            "                criteria.addOrder(Order.asc(sort.get(\"property\")));\n" +
            "            } else {\n" +
            "                criteria.addOrder(Order.desc(sort.get(\"property\")));\n" +
            "            }\n" +
            "        }\n" +
            "\n" +
            "        List<${className}> objects = criteria.list();\n" +
            "        pagedList.setValue(objects);\n" +
            "        pagedList.setSize(total);\n" +
            "        return pagedList;"
    out.println "    }"
    out.println "}"
}

// repository
def static generateRepository(packageName, out, className, fields) {
    out.println "package $packageName;"
    out.println ""
    out.println "public interface ${className}Repository {"
    out.println "    ${className} find${className}ByUuid(String uuid);\n" +
            "    void save(${className} entity);\n" +
            "    void delete(${className} entity);\n" +
            "    void update(${className} entity);"
    out.println "}"
}

def static generateRepositoryImpl(packageName, out, className, fields) {
    out.println "package $packageName;"
    out.println "import com.shinow.abc.common.domain.port.adapter.persistence.hibernate.AbstractHibernateSession;"
    out.println "import org.hibernate.Session;"
    out.println ""
    out.println "public class Hibernate${className}Repository extends AbstractHibernateSession implements ${className}Repository {"
    out.println "    public Hibernate${className}Repository(Session session) {\n" +
            "        super(session);\n" +
            "    }"
    out.println "    @Override\n" +
            "    public ${className} find${className}ByUuid(String uuid) {\n" +
            "        return (${className}) this.session.get(${className}.class, uuid);\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public void save(${className} entity) {\n" +
            "        this.session.save(entity);\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public void delete(${className} entity) {\n" +
            "        this.session.delete(entity);\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public void update(${className} entity) {\n" +
            "        ${className} oldentity= this.find${className}ByUuid(entity.getUuid());\n" +
            "        this.session.evict(oldentity);\n" +
            "        this.session.update(entity);\n" +
            "    }"
    out.println "}"
}

// pojo
def static generatePojo(packageName, out, className, fields) {
    out.println "package $packageName;"
    out.println ""
    out.println "import com.shinow.abc.common.domain.Entity;"
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
    out.println "public class $className extends Entity{"
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

// --------本就有的方法
def calcFields(table) {
    DasUtil.getColumns(table).reduce([]) { fields, col ->
        def spec = Case.LOWER.apply(col.getDataType().getSpecification())
        def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
        fields += [[
                           name : javaName(col.getName(), false),
                           type : typeStr,
                           colName    : col.getName(), // 表的列名
                           colDateType: col.getDataType(),
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