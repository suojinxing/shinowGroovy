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
    new File(dir, className + ".js").withPrintWriter { out -> generate(out, className, fields) }
}

def generate(out, className, fields) {
    out.println("Ext.define('Amili.xxxxxmodelxxxxx.model.', {")
    out.println("    extend: 'Ext.data.Model',")
    out.println("    idProperty: 'uuid',")
    out.println("   ")
    out.println("    fields: [{")
    for (int i = 0; i < fields.size(); i++) {
        def name = fields.get(i)["name"]
        if (fields.get(i)["type"] == "Date") {
            out.println "        name: '" + name + "',"
            out.println "        type: 'date',"
            out.println "        dateFormat: 'Y-m-d H:i:s'"
            if (i == fields.size() - 1) {
                out.println "    }],"
            } else {
                out.println "    }, {"
            }
            continue;
        }
        out.println "        name: '" + name + "'"
        if (i == fields.size() - 1) {
            out.println "    }],"
        } else {
            out.println "    }, {"
        }
    }
    out.println("    proxy: {")
    out.println("        type: 'ajax',")
    out.println("        extraParams: {")
    out.println("            'source': 'amili'")
    out.println("        },")
    out.println("        api: {")
    out.println("        read: './xxxxxmodelxxxxx/xxxxxmodelxxxxx',")
    out.println("        update: './xxxxxmodelxxxxx/update',")
    out.println("        destroy: './xxxxxmodelxxxxx/delete'")
    out.println("    },")
    out.println("        writer: {")
    out.println("            writeAllFields: true")
    out.println("        },")
    out.println "        reader: {"
    out.println "            rootProperty: 'object'"
    out.println "        }"
    out.println "    }"
    out.println("});")
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
