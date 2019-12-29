import com.intellij.database.model.DasTable
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

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

def generate(table, dir) {
  def className = javaName(table.getName(), true)
  def fields = calcFields(table)
  new File(dir, className + ".hbm.xml").withPrintWriter { out -> generate(out, className, fields, table) }
}

def static generate(out, className, fields, table) {
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

def calcFields(table) {
  DasUtil.getColumns(table).reduce([]) { fields, col ->
    def spec = Case.LOWER.apply(col.getDataType().getSpecification())
    def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
    fields += [[
                       name       : javaName(col.getName(), false), // 列名对应的Java名
                       type       : typeStr,
                       colName    : col.getName(), // 表的列名
                       colDateType: col.getDataType(),
                       annos      : ""]]
  }
}

// 获取java风格的命名。
def javaName(str, capitalize) {
  def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
          .collect { Case.LOWER.apply(it).capitalize() }
          .join("")
          .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
  capitalize || s.length() == 1 ? s : Case.LOWER.apply(s[0]) + s[1..-1]
}