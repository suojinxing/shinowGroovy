import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

// ---------------------------------------------------------------------------------
// 自定义配置项，只配置数据库前缀长度和模块的包名
// T_MSC_AU  ===> prefixLength=6   长度按字母算，下划线不做计数
//projectPath = "project_absolute_path_eg_user_shinow_abc"
//rootModelDir = "modelname_eg_materialmanagent";
prefixLength = 6
projectPath = "/Users/apple/shinowProject/abc-lims-msc-pluripotent/src/main/java/com/shinow/abc"
rootModelDir = "autologousoutstore";

// "包名"
packageName = ""

/*
 * 将路径平台化，适应window、MacOS多平台化
 */
def platformPtath(path){
    return path.replaceAll("\\\\","/")
}

// 将路径平台化
projectPath = platformPtath(projectPath)
rootModelDir = platformPtath(rootModelDir)

// 获取view所在的目录public文件夹的绝对路径
def getResourcePublicPath(projectPathOrPublicPath) {
    ///Users/apple/shinowProject/abc-lims-msc-pluripotent/src/main/resources/public
    ///Users/apple/shinowProject/abc-lims-msc-pluripotent/src/main/java/com/shinow/abc
    if (projectPathOrPublicPath.endsWith("/main/resources/public")) {
        return projectPathOrPublicPath
    }
    return projectPath.replace("/main/java/com/shinow/abc", "/main/resources/public")
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

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
    SELECTION.filter { it instanceof DasTable }.each { generate(it, dir) }
}

def generate(table, dir) {
    def generateDir
    def className = javaName(table.getName(), true)
    className = className.substring(prefixLength)
    def fields = calcFields(table)
//    packageName = getPackageName(dir);

    // 定义文件夹
    File model = new File(getResourcePublicPath(projectPath) + "/" + rootModelDir);
    File view = new File(getResourcePublicPath(projectPath) + "/" + rootModelDir + "/view");
    File imgs = new File(getResourcePublicPath(projectPath) + "/" + rootModelDir + "/imgs");
    File subModel = new File(getResourcePublicPath(projectPath) + "/" + rootModelDir + "/model");

    List<File> fileList = new ArrayList<>();
    fileList.add(model);
    fileList.add(view);
    fileList.add(imgs);
    fileList.add(subModel);

    // 生成文件夹
    for (int i = 0; i < fileList.size(); i++) {
        fileList.get(i).mkdirs();
    }

    // 生成文件夹对应的文件service、pojo、repository
    for (int i = 0; i < fileList.size(); i++) {
        String packdir = fileList.get(i).getName();
        generateDir = new File(getResourcePublicPath(projectPath) + "/" + rootModelDir + "/" + packdir); // 绝对路径
        packageName = getPackageName(generateDir); // 在这里貌似没什么用
        switch (packdir) {
            case "model":
                // 生成model.js
                new File(generateDir, className + ".js").withPrintWriter { out -> generateModelJs(packageName, out, className, fields) }
                break;
            case "imgs":
                // 生成图片
                break;
            case "view":
                // 生成View、ViewController、ViewModel|DeteilView、DetailViewController、DetailViewModel
                new File(generateDir, "ListView.js").withPrintWriter { out -> generateListViewJs(packageName, out, className, fields) }
                new File(generateDir, "ListViewController.js").withPrintWriter { out -> generateListViewControllerJs(packageName, out, className, fields) }
                new File(generateDir, "ListViewModel.js").withPrintWriter { out -> generateListViewModelJs(packageName, out, className, fields) }
                new File(generateDir, "DetailView.js").withPrintWriter { out -> generateDeatilViewJs(packageName, out, className, fields) }
                new File(generateDir, "DetailViewController.js").withPrintWriter { out -> generateDeteilViewControllerJs(packageName, out, className, fields) }
                new File(generateDir, "DetailViewModel.js").withPrintWriter { out -> generateDetialViewModelJs(packageName, out, className, fields) }
                break;
        }
    }
    // 生成模块下的Main.js
    def generateMainDir = new File(getResourcePublicPath(projectPath) + "/" + rootModelDir)
    new File(generateMainDir, "Main.js").withPrintWriter { out -> generateMainJs(packageName, out, className, fields) }


}
// 获取类名的第一个字母，并把它变成小写字母返回。
def getFirstCharToLowerJavaName(className) {
    return "${(char) (className.charAt(0) + 32)}${className.substring(1)}"
}

// Main.js
def generateMainJs(packageName, out, className, fields) {
    out.println "Ext.define('Amili.${rootModelDir}.Main', {\n" +
            "    extend: 'Ext.container.Container',\n" +
            "    alias: 'widget.${rootModelDir}-main',\n" +
            "\n" +
            "    itemId: '${rootModelDir}',\n" +
            "\n" +
            "    requires: [\n" +
            "        'Amili.${rootModelDir}.view.ListView'\n" +
            "    ],\n" +
            "\n" +
            "    items: [{\n" +
            "        xtype: '${rootModelDir}-view-list'\n" +
            "    }]\n" +
            "});"
}

// DetialViewModel
def generateDetialViewModelJs(packageName, out, className, fields) {
    out.println "Ext.define('Amili.${rootModelDir}.view.DetailViewModel', {\n" +
            "    extend: 'Ext.app.ViewModel',\n" +
            "    alias: 'viewmodel.${rootModelDir}-detail',\n" +
            "    requires: [],\n" +
            "    stores: {\n" +
            "        operators: {\n" +
            "            fields: ['value', 'name'],\n" +
            "            autoLoad: true,\n" +
            "            proxy: {\n" +
            "                type: 'ajax',\n" +
            "                extraParams: {\n" +
            "                    'source': 'amili'\n" +
            "                },\n" +
            "                url: './${rootModelDir}/operators',\n" +
            "                reader: {\n" +
            "                    type: 'json',\n" +
            "                    rootProperty: 'objects'\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "});"
}

// DetailViewController
def generateDeteilViewControllerJs(packageName, out, className, fields) {
    out.println "Ext.define('Amili.${rootModelDir}.view.DetailViewController', {\n" +
            "    extend: 'Ext.app.ViewController',\n" +
            "    alias: 'controller.${rootModelDir}-detail',\n" +
            "\n" +
            "    requires: [\n" +
            "        'Amili.amili.ux.FlyTip'\n" +
            "    ],\n" +
            "\n" +
            "    init() {},\n" +
            "    cancel(button, e, eOpts) {\n" +
            "        this.getView().close();\n" +
            "    }\n" +
            "});"
}

// DetialView
def generateDeatilViewJs(packageName, out, className, fields) {
    out.println "Ext.define('Amili.${rootModelDir}.view.DetailView', {\n" +
            "    extend: 'Ext.form.Panel',\n" +
            "    alias: 'widget.${rootModelDir}-view-detail',\n" +
            "\n" +
            "    requires: [\n" +
            "        'Amili.${rootModelDir}.view.DetailViewController',\n" +
            "        'Amili.${rootModelDir}.view.DetailViewModel'\n" +
            "    ],\n" +
            "\n" +
            "    bodyPadding: 5,\n" +
            "    floating: true,\n" +
            "    closable: true,\n" +
            "    modal: true,\n" +
            "    frame: true,\n" +
            "    title: '详情',\n" +
            "\n" +
            "    controller: '${rootModelDir}-detail',\n" +
            "    viewModel: {\n" +
            "        type: '${rootModelDir}-detail'\n" +
            "    },\n" +
            "    defaults: {\n" +
            "        anchor: '100%'\n" +
            "    },\n"
    out.println("    items: [{")
    out.println("        layout: 'vbox',")
    out.println "        xtype: 'fieldcontainer',"
    out.println "        items: [{"
    for (int i = 0; i < fields.size(); i++) {
        if (fields.get(i)["name"] == "uuid") {
            continue;
        }
        out.println "            fieldLabel: '" + fields.get(i)["comment"] + "',"
        out.println "            labelAlign: 'right',"
        out.println "            readOnly: true,"
        out.println "            labelWidth: Amili.tickMark(9),"
        out.println "            awidth: Amili.tickMark(25),"

        if ((fields.get(i)["name"] + "").endsWith("or") || (fields.get(i)["name"] + "").endsWith("er")) {
            out.println "            xtype: 'combobox',"
            out.println "            displayField: 'name',"
            out.println "            valueField: 'value',"
            out.println "            bind: {"
            out.println "                store: '{operators}',"
            out.println "                value: '{${getFirstCharToLowerJavaName(className)}.${fields.get(i)["name"]}}'"
            out.println "            }"
        } else if (fields.get(i)["type"] == "Date") {
            out.println "            xtype: 'datefield',"
            out.println "            format: 'Y-m-d H:i:s',"
            out.println "            bind: '{${getFirstCharToLowerJavaName(className)}.${fields.get(i)["name"]}}'"
        } else {
            out.println "            xtype: 'textfield',"
            out.println "            bind: '{${getFirstCharToLowerJavaName(className)}.${fields.get(i)["name"]}}'"
        }
        if (i == fields.size() - 1) {
            out.println "        }],"
            continue
        }
        out.println "        }, {"
    }
    out.println "    }],"
    out.println "    buttons: ['->', {\n" +
            "        text: '取消',\n" +
            "        name: 'cancel',\n" +
            "        listeners: {\n" +
            "            click: 'cancel'\n" +
            "        }\n" +
            "    }]\n" +
            "});"
}

// ListViewModel
def generateListViewModelJs(packageName, out, className, fields) {
    out.println "Ext.define('Amili.${rootModelDir}.view.ListViewModel', {\n" +
            "    extend: 'Ext.app.ViewModel',\n" +
            "    alias: 'viewmodel.${rootModelDir}-list',\n" +
            "\n" +
            "    requires: [\n" +
            "        'Amili.${rootModelDir}.model.${className}'\n" +
            "    ],\n" +
            "\n" +
            "    stores: {\n" +
            "        ${rootModelDir}s: {\n" +
            "            model: 'Amili.${rootModelDir}.model.${className}',\n" +
            "            autoLoad: false,\n" +
            "            remoteFilter: true,\n" +
            "            remoteSort: true,\n" +
            "            proxy: {\n" +
            "                type: 'ajax',\n" +
            "                extraParams: {\n" +
            "                    'source': 'amili'\n" +
            "                },\n" +
            "                url: './${rootModelDir}/${rootModelDir}s',\n" +
            "                reader: {\n" +
            "                    type: 'json',\n" +
            "                    rootProperty: 'objects'\n" +
            "                }\n" +
            "            }\n" +
            "        },\n" +
            "        operators: {\n" +
            "            fields: ['value', 'name'],\n" +
            "            autoLoad: false,\n" +
            "            proxy: {\n" +
            "                type: 'ajax',\n" +
            "                extraParams: {\n" +
            "                    'source': 'amili'\n" +
            "                },\n" +
            "                url: './${rootModelDir}/operators',\n" +
            "                reader: {\n" +
            "                    type: 'json',\n" +
            "                    rootProperty: 'objects'\n" +
            "                }\n" +
            "            }\n" +
            "        },\n" +
            "        // 字典模板，只做参照，请自行替换\n" +
            "        dictionaries: {\n" +
            "            fields: ['id', 'name'],\n" +
            "            data: [{\n" +
            "                id: 1,\n" +
            "                name: '是'\n" +
            "            }, {\n" +
            "                id: 0,\n" +
            "                name: '否'\n" +
            "            }]\n" +
            "        }\n" +
            "    }\n" +
            "});"
}

// ListViewController
def generateListViewControllerJs(packageName, out, className, fields) {
    out.println "Ext.define('Amili.${rootModelDir}.view.ListViewController', {\n" +
            "    extend: 'Ext.app.ViewController',\n" +
            "    alias: 'controller.${rootModelDir}-list',\n" +
            "\n" +
            "    requires: [\n" +
            "        'Amili.amili.ux.FlyTip'\n" +
            "    ],\n" +
            "\n" +
            "    listen: {\n" +
            "        controller: {\n" +
            "            //'${rootModelDir}-context-menu': {\n" +
            "            //    ${rootModelDir}deletesuccess: 'doRefresh'\n" +
            "            //},\n" +
            "            //'${rootModelDir}-edit': {\n" +
            "            //    ${rootModelDir}editsuccess: 'doRefresh'\n" +
            "            //},\n" +
            "            //'${rootModelDir}-add': {\n" +
            "            //    ${rootModelDir}addsuccess: 'doRefresh'\n" +
            "            //}\n" +
            "\n" +
            "        }\n" +
            "    },\n" +
            "\n" +
            "    init() {\n" +
            "        let me = this;\n" +
            "        Ext.Deferred.all(me.loadOperatorsStore()).always(function () {\n" +
            "            me.loadAutologous${className}Store().otherwise(function (operation) {\n" +
            "                let responseText = Ext.decode(operation.getResponse().responseText);\n" +
            "                Amili.amili.FlyTip.fly(responseText.title, responseText.message, responseText.category);\n" +
            "            });\n" +
            "        });\n" +
            "    },\n" +
            "\n" +
            "    loadAutologous${className}Store() {\n" +
            "        let me = this\n" +
            "        let deferred = Ext.create('Ext.Deferred')\n" +
            "        let store = this.getViewModel().getStore('${rootModelDir}s')\n" +
            "\n" +
            "        store.on('beforeload', function (store, operation, eOpts) {\n" +
            "            let pageSize = this.getView().calculatePageSize();\n" +
            "            operation.setLimit(pageSize);\n" +
            "            store.setPageSize(pageSize);\n" +
            "        }, this);\n" +
            "        store.load({\n" +
            "            callback(records, operation, success) {\n" +
            "                if (success) {\n" +
            "                    deferred.resolve(records);\n" +
            "                } else {\n" +
            "                    deferred.reject(operation);\n" +
            "                }\n" +
            "            }\n" +
            "        });\n" +
            "        return deferred.promise;\n" +
            "    },\n" +
            "\n" +
            "    loadOperatorsStore() {\n" +
            "        let deferred = Ext.create('Ext.Deferred')\n" +
            "        let store = this.getViewModel().getStore('operators');\n" +
            "        store.load({\n" +
            "            callback(records, operation, success) {\n" +
            "                if (success) {\n" +
            "                    deferred.resolve(records);\n" +
            "                } else {\n" +
            "                    deferred.reject(operation);\n" +
            "                }\n" +
            "            }\n" +
            "        });\n" +
            "        return deferred.promise;\n" +
            "    },\n" +
            "\n" +
            "    doRefresh() {\n" +
            "        this.getView().getStore().reload();\n" +
            "    },\n" +
            "\n" +
            "    doUpdate(startDate, endDate, date) {\n" +
            "        this.lookupReference('selectedDay').setText(date);\n" +
            "        this.getViewModel().set('startDate', startDate);\n" +
            "        this.getViewModel().set('endDate', endDate);\n" +
            "        this.lookupReference('searchButton').fireEvent('click', this.lookupReference('searchButton'));\n" +
            "    },\n" +
            "\n" +
            "    search(button, e, eOpts) {\n" +
            "        let searchFields = button.up('toolbar').query('textfield')\n" +
            "        let store = this.getViewModel().getStore('${rootModelDir}s')\n" +
            "        store.clearFilter(true);\n" +
            "        Ext.Array.each(searchFields, function (searchField, index, searchFields) {\n" +
            "            if (searchField.isValid() && searchField.getValue() !== '' && searchField.getValue() !== null) {\n" +
            "                let filter = Ext.create('Ext.util.Filter', {\n" +
            "                    property: searchField.getName(),\n" +
            "                    value: searchField.getValue()\n" +
            "                });\n" +
            "                store.addFilter(filter, true);\n" +
            "            }\n" +
            "        });\n" +
            "        //if (this.getViewModel().get('startDate') !== null && this.getViewModel().get('startDate') !== '') {\n" +
            "        //    let filter1 = Ext.create('Ext.util.Filter', {\n" +
            "        //        property: 'startDate',\n" +
            "        //        value: this.getViewModel().get('startDate')\n" +
            "        //    });\n" +
            "        //    store.addFilter(filter1, true);\n" +
            "        //}\n" +
            "\n" +
            "        //if (this.getViewModel().get('endDate') !== null && this.getViewModel().get('endDate') !== '') {\n" +
            "        //    let filter2 = Ext.create('Ext.util.Filter', {\n" +
            "        //        property: 'endDate',\n" +
            "        //        value: this.getViewModel().get('endDate')\n" +
            "        //    });\n" +
            "        //    store.addFilter(filter2, true);\n" +
            "        //}\n" +
            "        store.load({\n" +
            "            callback(records, operation, success) {\n" +
            "                if (!success) {\n" +
            "                    let responseText = Ext.decode(operation.getResponse().responseText);\n" +
            "                    Amili.amili.FlyTip.fly(responseText.title, responseText.message, responseText.category);\n" +
            "                }\n" +
            "            },\n" +
            "            scope: this\n" +
            "        });\n" +
            "    },\n" +
            "\n" +
            "    reset(button, e, eOpts) {\n" +
            "        let searchFields = button.up('toolbar').query('textfield')\n" +
            "        let store = this.getViewModel().getStore('${rootModelDir}s')\n" +
            "\n" +
            "        Ext.Array.each(searchFields, function (searchField, index, searchFields) {\n" +
            "            searchField.setValue('');\n" +
            "        });\n" +
            "        store.clearFilter();\n" +
            "        !!store.sorters && store.sorters.clear();\n" +
            "        store.currentPage = 1;\n" +
            "\n" +
            "        store.load({\n" +
            "            callback(records, operation, success) {\n" +
            "                if (!success) {\n" +
            "                    let responseText = Ext.decode(operation.getResponse().responseText);\n" +
            "                    Amili.amili.FlyTip.fly(responseText.title, responseText.message, responseText.category);\n" +
            "                }\n" +
            "            },\n" +
            "            scope: this\n" +
            "        });\n" +
            "    },\n" +
            "    itemdblclick(data, item, index, e, eOpts) {\n" +
            "        let uuid = item.data.uuid;\n" +
            "        let record = Ext.create('Amili.${rootModelDir}.model.${className}', {});\n" +
            "\n" +
            "        record.load({\n" +
            "            scope: this,\n" +
            "            params: {uuid: uuid},\n" +
            "            failure(response, options) {},\n" +
            "            success(record, operation) {\n" +
            "                let panel = Ext.create('Amili.${rootModelDir}.view.DetailView', {\n" +
            "                    width: Amili.tickMark(30, 1),\n" +
            "                    height: Amili.tickMark(30, 2),\n" +
            "                    scrollable: 'y',\n" +
            "                    viewModel: {\n" +
            "                        data: {\n" +
            "                            ${getFirstCharToLowerJavaName(className)}: record,\n" +
            "                        }\n" +
            "                    }\n" +
            "                });\n" +
            "                panel.show();\n" +
            "            }\n" +
            "        });\n" +
            "    },\n" +
            "});"
}

// ListView
def generateListViewJs(packageName, out, className, fields) {
    out.println("Ext.define('Amili.${rootModelDir}.view.ListView', {")
    out.println("        extend: 'Ext.grid.Panel',")
    out.println("        alias: 'widget.${rootModelDir}-view-list',")
    out.println("       ")
    out.println("        requires: [")
    out.println("            'Amili.${rootModelDir}.view.ListViewModel',")
    out.println("            'Amili.${rootModelDir}.view.ListViewController',")
    out.println("            //'Amili.${rootModelDir}.view.DateMenuView'")
    out.println("        ],")
    out.println("        listeners: {")
    out.println("            itemdblclick: 'itemdblclick',")
    out.println("        },")
    out.println("        viewConfig: {enableTextSelection: true},")
    out.println("        controller: '${rootModelDir}-list',")
    out.println("        viewModel: {")
    out.println("            type: '${rootModelDir}-list'")
    out.println("        },")
    out.println("        enableColumnHide: false,")
    out.println("        reference: '${rootModelDir}list',")
    out.println("        bind: {")
    out.println("            store: '{${rootModelDir}s}'")
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
        if ((fields.get(i)["name"] + "").endsWith("or") || (fields.get(i)["name"] + "").endsWith("er")) {
            out.println "            //renderer(value) {"
            out.println "                //let store = this.getViewModel().getStore('operators')"
            out.println "                //let idx = store.findExact('value', value)"
            out.println "                //let record = store.getAt(idx)"
            out.println "                //return record === null || record === undefined ? '' : record.data.name;"
            out.println "            //}"
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
    out.println("                //url: './${rootModelDir}/listtoolbar?theme=' + Ext.themeName,")
    out.println("                //renderer: 'component'")
    out.println("            }")
    out.println("        }, {")
    out.println("            xtype: 'pagingtoolbar',")
    out.println("            bind: {")
    out.println("                store: '{${rootModelDir}s}'")
    out.println("            },")
    out.println("            dock: 'bottom',")
    out.println("            displayInfo: true")
    out.println("        }]")
    out.println("    }, () => {")
    out.println("        'use strict';")
    out.println("        Ext.util.CSS.swapStyleSheet('Amili.${rootModelDir}.view.ListView', Amili.getApplication().paths['Amili.${rootModelDir}'] ? './${rootModelDir}/view/ListView.css' : './webjars/${rootModelDir}/view/ListView.css');")
    out.println("    }")
    out.println(");    ")
}

// 生成model.js
def generateModelJs(packageName, out, className, fields) {
    out.println("Ext.define('Amili.${rootModelDir}.model.$className', {")
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
    out.println("        read: './${rootModelDir}/${rootModelDir}',")
    out.println("        update: './${rootModelDir}/update',")
    out.println("        destroy: './${rootModelDir}/delete'")
    out.println("    },")
    out.println("        writer: {")
    out.println("            writeAllFields: true")
    out.println("        },")
    out.println "        reader: {"
    out.println "            type: 'json',"
    out.println "            rootProperty: 'object'"
    out.println "        }"
    out.println "    }"
    out.println("});")
}

// --------本就有的方法------
def calcFields(table) {
    DasUtil.getColumns(table).reduce([]) { fields, col ->
        def spec = Case.LOWER.apply(col.getDataType().getSpecification())
        def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
        fields += [[
                           name       : javaName(col.getName(), false),
                           type       : typeStr,
                           colName    : col.getName(), // 表的列名
                           colDateType: col.getDataType(),
                           comment    : col.getComment(),
                           annos      : ""]]
    }
}

def javaName(str, capitalize) {
    def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
            .collect { Case.LOWER.apply(it).capitalize() }
            .join("")
            .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
    capitalize || s.length() == 1 ? s : Case.LOWER.apply(s[0]) + s[1..-1]
}