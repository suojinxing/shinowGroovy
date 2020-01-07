import com.intellij.database.model.DasTable
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

import java.lang.String

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
    new File(dir, "items.js").withPrintWriter("utf-8") { out -> generate(out, className, fields) }
}

def generate(out, className, fields) {
    out.println("        items: [{")
    out.println("            layout: 'vbox',")
    out.println "            xtype: 'fieldcontainer',"
    out.println "            items: [{"
    for (int i = 0; i < fields.size(); i++) {
        out.println "            fieldLabel:'" + fields.get(i)["comment"] + "',"
        if (fields.get(i)["type"] == "Date") {
            out.println "            xtype: 'datefield',"
            out.println "            format: 'Y-m-d H:i:s',"
        }
        out.println "            labelAlign: 'right',"
        out.println "            readOnly: true,"
        out.println "            labelWidth: Amili.tickMark(9),"
        out.println "            awidth: Amili.tickMark(25),"

        if((fields.get(i)["name"]+"").endsWith("or") || (fields.get(i)["name"]+"").endsWith("er")){
            out.println "            displayField: 'name',"
            out.println "            valueField: 'value',"
            out.println "            bind: {"
            out.println "                store: '{operators}',"
            out.println "                value: '{xxxxxxxxx.${fields.get(i)["name"]}}"
            out.println "            }"
        }else{
            out.println "            bind: '{xxxxxxxxx.${fields.get(i)["name"]}}'"
        }
        if (i == fields.size() - 1) {
            out.println "        }],"
            continue
        }
        out.println "        }, {"
    }
}

def calcFields(table) {
    DasUtil.getColumns(table).reduce([]) { fields, col ->
        def spec = Case.LOWER.apply(col.getDataType().getSpecification())
        def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
        fields += [[
                           name   : javaName(col.getName(), false),
                           type   : typeStr,
                           comment: col.getComment(),
                           annos  : ""]]
    }
}

def javaName(str, capitalize) {
    def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
            .collect { Case.LOWER.apply(it).capitalize() }
            .join("")
            .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
    capitalize || s.length() == 1 ? s : Case.LOWER.apply(s[0]) + s[1..-1]
}
