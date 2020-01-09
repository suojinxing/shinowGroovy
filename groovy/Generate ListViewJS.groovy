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
    new File(dir, "ListView.js").withPrintWriter("utf-8") { out -> generate(out, className, fields) }
}

def generate(out, className, fields) {
    out.println("Ext.define('Amili.xxxxviewpathxxxx.view.ListView', {")
    out.println("        extend: 'Ext.grid.Panel',")
    out.println("        alias: 'widget.xxxxviewpathxxxx-view-list',")
    out.println("       ")
    out.println("        requires: [")
    out.println("            //'Amili.xxxxviewpathxxxx.view.ListViewModel',")
    out.println("            //'Amili.xxxxviewpathxxxx.view.ListViewController',")
    out.println("            //'Amili.xxxxviewpathxxxx.view.DateMenuView'")
    out.println("        ],")
    out.println("        listeners: {")
    out.println("            itemdblclick: 'itemdblclick',")
    out.println("        },")
    out.println("        viewConfig: {enableTextSelection: true},")
    out.println("        //controller: 'xxxxviewpathxxxx-list',")
    out.println("        viewModel: {")
    out.println("            //type: 'xxxxviewpathxxxx-list'")
    out.println("        },")
    out.println("        enableColumnHide: false,")
    out.println("        reference: 'xxxxviewpathxxxxlist',")
    out.println("        bind: {")
    out.println("            //store: '{xxxxstoresxxxx}'")
    out.println("        },")
    out.println("        columns: [{")
    out.println("            xtype: 'rownumberer',")
    out.println "            width: Amili.tickMark(2.5)"
    out.println "        }, {"
    for (int i = 0; i < fields.size(); i++) {
        out.println "            text:'" + fields.get(i)["comment"] + "',"
        if (fields.get(i)["type"] == "Date") {
            out.println "            xtype: 'datecolumn',"
            out.println "            format: 'Y-m-d H:i:s',"
        }
        out.println "            width: Amili.tickMark(8),"
        out.println "            menuDisabled: true,"
        out.println "            sortable: false,"
        out.println "            align: 'center',"
        out.println "            dataIndex: '" + fields.get(i)["name"] + "',"
        if((fields.get(i)["name"]+"").endsWith("or") || (fields.get(i)["name"]+"").endsWith("er")){
            out.println "            renderer(value) {"
            out.println "                let store = this.getViewModel().getStore('operators')"
            out.println "                let idx = store.findExact('value', value)"
            out.println "                let record = store.getAt(idx)"
            out.println "                return record === null || record === undefined ? '' : record.data.name;"
            out.println "            }"
        }
        if (i == fields.size() - 1) {
            out.println "        }],"
            continue
        }
        out.println "        }, {"
    }
    out.println("        dockedItems: [{")
    out.println("            loader: {")
    out.println("                //autoLoad: true,")
    out.println("                //url: './xxxxviewpathxxxx/listtoolbar?theme=' + Ext.themeName,")
    out.println("                //renderer: 'component'")
    out.println("            }")
    out.println("        }, {")
    out.println("            xtype: 'pagingtoolbar',")
    out.println("            bind: {")
    out.println("                //store: '{xxxxstoresxxxx}'")
    out.println("            },")
    out.println("            dock: 'bottom',")
    out.println("            displayInfo: true")
    out.println("        }]")
    out.println("    }, () => {")
    out.println("        'use strict';")
    out.println("        Ext.util.CSS.swapStyleSheet('Amili.xxxxviewpathxxxx.view.ListView', Amili.getApplication().paths['Amili.xxxxviewpathxxxx'] ? './xxxxviewpathxxxx/view/ListView.css' : './webjars/xxxxviewpathxxxx/view/ListView.css');")
    out.println("    }")
    out.println(");    ")
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
