import com.intellij.database.model.DasTable
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


// 自定义配置项
prefixLength = 6

projectPath = "/Users/apple/shinowProject/abc-lims-msc-pluripotent/src/main/java/com/shinow/abc"
rootModelDir = "xxxmodelxxx";
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
    className = className.substring(prefixLength)

    def fields = calcFields(table)
    packageName = getPackageName(dir);

    // 定义文件夹
    File model = new File(projectPath + "/" + rootModelDir);
    // 生成文件夹
    model.mkdirs();
    // 生成action、bundle等公共的类
    File generateActionDir = new File(projectPath + "/" + rootModelDir);
    File generateBundleDir = new File(projectPath)
    new File(generateActionDir, className + "sAction.java").withPrintWriter { out -> generatePojosAction(packageName, out, className, fields) }
    new File(generateActionDir, className + "Action.java").withPrintWriter { out -> generatePojoAction(packageName, out, className, fields) }
    new File(generateActionDir, "OperatorsAction.java").withPrintWriter { out -> generateOperatorsAction(packageName, out, className, fields) }
    new File(generateActionDir, "ContextMenuAction.java").withPrintWriter { out -> generateMenuAction(packageName, out, className, fields) }
    new File(generateBundleDir, className + "Bundle.java").withPrintWriter { out -> generateBundle(packageName, out, className, fields) }
}

// 获取类名的第一个字母，并把它变成小写字母返回。
def getFirstCharToLower(className){
    return (char)(className.charAt(0)+32)
}

def generateMenuAction(packageName, out, className, fields){
    out.println "package $packageName.${rootModelDir};"
    out.println "\n" +
            "import com.fasterxml.jackson.databind.ObjectMapper;\n" +
            "import com.shinow.abc.amili.bundle.Action;\n" +
            "import com.shinow.abc.amili.security.UserDescriptor;\n" +
            "import com.shinow.abc.common.domain.infrastructure.persistence.SessionManager;\n" +
            "import com.shinow.abc.msc.${rootModelDir}.domain.model.${className};\n" +
            "import org.hibernate.Session;\n" +
            "import org.hibernate.Transaction;\n" +
            "\n" +
            "import javax.servlet.http.HttpServletRequest;\n" +
            "import javax.servlet.http.HttpServletResponse;\n" +
            "import java.util.ArrayList;\n" +
            "import java.util.HashMap;\n" +
            "import java.util.List;\n" +
            "\n" +
            "public class ContextMenuAction implements Action {\n" +
            "\n" +
            "    @Override\n" +
            "    public String getId() {\n" +
            "        return \"${rootModelDir}contextmenu\";\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public String getName() {\n" +
            "        return \"右键菜单\";\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public String getDescription() {\n" +
            "        return \"返回右键菜单\";\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public void execute(HttpServletRequest request, HttpServletResponse response, UserDescriptor userDescriptor) throws Exception {\n" +
            "        ObjectMapper mapper = new ObjectMapper();\n" +
            "        String uuid = request.getParameter(\"uuid\");\n" +
            "        Session session = SessionManager.getInstance().getCurrentSession(\"autologousmsc\");\n" +
            "        Transaction tx = null;\n" +
            "        ${className} ${getFirstCharToLower(className)}${className.substring(1)} = null;\n" +
            "        try {\n" +
            "            tx = session.beginTransaction();\n" +
            "            EntityRepository<${className}> repository = new HibernateEntityRepository<>(session);\n" +
            "            ${getFirstCharToLower(className)}${className.substring(1)} = repository.findById(uuid, ${className}.class);\n" +
            "            tx.commit();\n" +
            "        } catch (Exception e) {\n" +
            "            if (tx != null) {\n" +
            "                tx.rollback();\n" +
            "            }\n" +
            "        }\n" +
            "\n" +
            "        List menuItems = new ArrayList();\n" +
            "        HashMap<String, Object> menuSeparator = new HashMap<>();\n" +
            "        if (${getFirstCharToLower(className)}${className.substring(1)} != null) {\n" +
            "            menuSeparator.put(\"xtype\", \"menuseparator\");\n" +
            "            if (userDescriptor.can(\"${rootModelDir}_manage\")) {\n" +
            "                HashMap<String, Object> editMenu = new HashMap<>();\n" +
            "                HashMap<String, Object> editMenuListeners = new HashMap<>();\n" +
            "                editMenu.put(\"text\", \"编辑\");\n" +
            "                editMenu.put(\"iconCls\", \"${rootModelDir}-context-menu-edit\");\n" +
            "                editMenuListeners.put(\"click\", \"edit\");\n" +
            "                editMenu.put(\"listeners\", editMenuListeners);\n" +
            "                menuItems.add(editMenu);\n" +
            "\n" +
            "                menuItems.add(menuSeparator);\n" +
            "                HashMap<String, Object> deleteMenu = new HashMap<>();\n" +
            "                HashMap<String, Object> deleteMenuListeners = new HashMap<>();\n" +
            "                deleteMenu.put(\"text\", \"删除\");\n" +
            "                deleteMenu.put(\"iconCls\", \"${rootModelDir}-context-menu-delete\");\n" +
            "                deleteMenuListeners.put(\"click\", \"del\");\n" +
            "                deleteMenu.put(\"listeners\", deleteMenuListeners);\n" +
            "                menuItems.add(deleteMenu);\n" +
            "            }\n" +
            "\n" +
            "            if (menuItems.size() == 0) {\n" +
            "                HashMap<String, Object> noOperate = new HashMap<>();\n" +
            "                noOperate.put(\"text\", \"无可用操作\");\n" +
            "                noOperate.put(\"iconCls\", \"${rootModelDir}-context-menu-no-operate\");\n" +
            "                menuItems.add(noOperate);\n" +
            "            }\n" +
            "        }\n" +
            "        response.setContentType(\"application/json\");\n" +
            "        response.setCharacterEncoding(\"utf-8\");\n" +
            "        response.getWriter().write(mapper.writeValueAsString(menuItems));\n" +
            "    }\n" +
            "\n" +
            "}"
}

def generatePojoAction(packageName, out, className, fields) {
    out.println "package $packageName.${rootModelDir};"
    out.println ""
    out.println "import com.fasterxml.jackson.annotation.JsonInclude;\n" +
            "import com.fasterxml.jackson.databind.ObjectMapper;\n" +
            "import com.shinow.abc.amili.bundle.Action;\n" +
            "import com.shinow.abc.amili.security.UserDescriptor;\n" +
            "import com.shinow.abc.common.domain.infrastructure.persistence.SessionManager;\n" +
            "import com.shinow.abc.common.domain.model.EntityRepository;\n" +
            "import com.shinow.abc.common.domain.model.HibernateEntityRepository;\n" +
            "import com.shinow.abc.msc.${rootModelDir}.domain.model.${className};\n" +
            "import org.hibernate.Session;\n" +
            "import org.hibernate.Transaction;\n" +
            "\n" +
            "import javax.servlet.http.HttpServletRequest;\n" +
            "import javax.servlet.http.HttpServletResponse;\n" +
            "import java.text.SimpleDateFormat;\n" +
            "import java.util.HashMap;"
    out.println ""
    out.println "public class ${className}Action implements Action {\n" +
            "    @Override\n" +
            "    public String getId() {\n" +
            "        return \"${rootModelDir}\";\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public String getName() {\n" +
            "        return \"查询\";\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public String getDescription() {\n" +
            "        return \"返回指定信息\";\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public void execute(HttpServletRequest request, HttpServletResponse response, UserDescriptor userDescriptor) throws Exception {\n" +
            "        String uuid = request.getParameter(\"uuid\");\n" +
            "        ObjectMapper mapper = new ObjectMapper();\n" +
            "        SimpleDateFormat df = new SimpleDateFormat(\"yyyy-MM-dd HH:mm:ss\");\n" +
            "        mapper.setDateFormat(df);\n" +
            "        mapper.setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.ALWAYS, JsonInclude.Include.NON_NULL));\n" +
            "        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);\n" +
            "        ${className} ${getFirstCharToLower(className)}${className.substring(1)};\n" +
            "        Session session = SessionManager.getInstance().getCurrentSession(\"xxxxxx_id__xxxxxxxx\");\n" +
            "        Transaction tx = null;\n" +
            "        try {\n" +
            "            tx = session.beginTransaction();\n" +
            "            EntityRepository<${className}> repository = new HibernateEntityRepository<>(session);\n" +
            "            ${getFirstCharToLower(className)}${className.substring(1)} = repository.findById(uuid, ${className}.class);\n" +
            "            tx.commit();\n" +
            "        } catch (Exception e) {\n" +
            "            if (tx != null) {\n" +
            "                tx.rollback();\n" +
            "            }\n" +
            "            throw e;\n" +
            "        }\n" +
            "        HashMap<String, Object> resultObject = new HashMap<>();\n" +
            "        resultObject.put(\"success\", true);\n" +
            "        resultObject.put(\"object\", ${getFirstCharToLower(className)}${className.substring(1)});\n" +
            "        response.setContentType(\"application/json\");\n" +
            "        response.setCharacterEncoding(\"utf-8\");\n" +
            "        response.getWriter().write(mapper.writeValueAsString(resultObject));\n" +
            "    }\n" +
            "}"

}

def generatePojosAction(packageName, out, className, fields) {
    out.println "package $packageName.${rootModelDir};"
    out.println "\n" +
            "import com.fasterxml.jackson.annotation.JsonInclude;\n" +
            "import com.fasterxml.jackson.core.type.TypeReference;\n" +
            "import com.fasterxml.jackson.databind.ObjectMapper;\n" +
            "import com.shinow.abc.amili.bundle.Action;\n" +
            "import com.shinow.abc.amili.security.UserDescriptor;\n" +
            "import com.shinow.abc.common.domain.infrastructure.PagedList;\n" +
            "import com.shinow.abc.common.domain.infrastructure.persistence.SessionManager;\n" +
            "import com.shinow.abc.msc.${rootModelDir}.domain.model.${className};\n" +
            "import com.shinow.abc.msc.${rootModelDir}.domain.service.${className}QueryService;\n" +
            "import com.shinow.abc.msc.${rootModelDir}.infrastructure.service.${className}QueryService;\n" +
            "import org.hibernate.Session;\n" +
            "import org.hibernate.Transaction;\n" +
            "\n" +
            "import javax.servlet.http.HttpServletRequest;\n" +
            "import javax.servlet.http.HttpServletResponse;\n" +
            "import java.text.DateFormat;\n" +
            "import java.text.SimpleDateFormat;\n" +
            "import java.util.ArrayList;\n" +
            "import java.util.HashMap;\n" +
            "import java.util.List;\n" +
            "\n" +
            "public class ${className}sAction implements Action {\n" +
            "    @Override\n" +
            "    public String getId() {\n" +
            "        return \"${rootModelDir}s\";\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public String getName() {\n" +
            "        return \"查找\";\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public String getDescription() {\n" +
            "        return \"返回所有xxxxxxxxgdhdhhdhdxxxxx信息\";\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public void execute(HttpServletRequest request, HttpServletResponse response, UserDescriptor userDescriptor) throws Exception {\n" +
            "        ObjectMapper mapper = new ObjectMapper();\n" +
            "        DateFormat df = new SimpleDateFormat(\"yyyy-MM-dd HH:mm:ss\");\n" +
            "        mapper.setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.ALWAYS, JsonInclude.Include.NON_NULL));\n" +
            "        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);\n" +
            "        mapper.setDateFormat(df);\n" +
            "        int start = Integer.parseInt(request.getParameter(\"start\"));\n" +
            "        int limit = Integer.parseInt(request.getParameter(\"limit\"));\n" +
            "        String sort = request.getParameter(\"sort\");\n" +
            "        String filter = request.getParameter(\"filter\");\n" +
            "        List<HashMap<String, String>> sorts = new ArrayList<>();\n" +
            "        List<HashMap<String, String>> filters = new ArrayList<>();\n" +
            "        if (sort != null) {\n" +
            "            sorts = mapper.readValue(sort, new TypeReference<List<HashMap>>() {\n" +
            "            });\n" +
            "        }\n" +
            "        if (filter != null) {\n" +
            "            filters = mapper.readValue(filter, new TypeReference<List<HashMap>>() {\n" +
            "            });\n" +
            "        }\n" +
            "        PagedList<${className}> pagedList = null;\n" +
            "        Session session = SessionManager.getInstance().getCurrentSession(\"xxxxxx__id__xxxxx\");\n" +
            "        Transaction tx = null;\n" +
            "        try {\n" +
            "            tx = session.beginTransaction();\n" +
            "            ${className}QueryService ${getFirstCharToLower(className)}${className.substring(1)}Service = new Hibernate${className}QueryService(session);\n" +
            "            pagedList = ${getFirstCharToLower(className)}${className.substring(1)}Service.find${className}s(start, limit, filters, sorts);\n" +
            "            tx.commit();\n" +
            "        } catch (Exception e) {\n" +
            "            if (tx != null) {\n" +
            "                tx.rollback();\n" +
            "            }\n" +
            "            throw e;\n" +
            "        }\n" +
            "        HashMap<String, Object> resultObject = new HashMap<>();\n" +
            "        resultObject.put(\"success\", true);\n" +
            "        resultObject.put(\"total\", pagedList.getSize());\n" +
            "        resultObject.put(\"objects\", pagedList.getValue());\n" +
            "        response.setContentType(\"application/json\");\n" +
            "        response.setCharacterEncoding(\"utf-8\");\n" +
            "        response.getWriter().write(mapper.writeValueAsString(resultObject));\n" +
            "    }\n" +
            "}"
}

def generateOperatorsAction(packageName, out, className, fields) {
    out.println "package $packageName.${rootModelDir};"
    out.println "\n" +
            "import com.fasterxml.jackson.annotation.JsonInclude;\n" +
            "import com.fasterxml.jackson.databind.ObjectMapper;\n" +
            "import com.shinow.abc.amili.bundle.Action;\n" +
            "import com.shinow.abc.amili.security.UserDescriptor;\n" +
            "import com.shinow.abc.common.domain.infrastructure.persistence.SessionManager;\n" +
            "import org.hibernate.SQLQuery;\n" +
            "import org.hibernate.Session;\n" +
            "import org.hibernate.Transaction;\n" +
            "import org.hibernate.transform.Transformers;\n" +
            "import org.hibernate.type.StringType;\n" +
            "\n" +
            "import javax.servlet.http.HttpServletRequest;\n" +
            "import javax.servlet.http.HttpServletResponse;\n" +
            "import java.util.HashMap;\n" +
            "import java.util.List;\n" +
            "import java.util.Map;\n" +
            "\n" +
            "public class OperatorsAction implements Action {\n" +
            "    @Override\n" +
            "    public String getId() {\n" +
            "        return \"operators\";\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public String getName() {\n" +
            "        return \"查询\";\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public String getDescription() {\n" +
            "        return \"返回所有用户\";\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public void execute(HttpServletRequest request, HttpServletResponse response, UserDescriptor userDescriptor) throws Exception {\n" +
            "        String sql = \"SELECT T.AVATAR AS value,T.NAME AS name FROM TBL_USER T\";\n" +
            "        Session session = SessionManager.getInstance().getCurrentSession(\"xxx_xxxx_id_xxxx\");\n" +
            "        Transaction tx = null;\n" +
            "        HashMap<String, Object> resultObject = new HashMap<>();\n" +
            "        try {\n" +
            "            tx = session.beginTransaction();\n" +
            "            SQLQuery query = session.createSQLQuery(sql)\n" +
            "                    .addScalar(\"value\", StringType.INSTANCE)\n" +
            "                    .addScalar(\"name\", StringType.INSTANCE);\n" +
            "            query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);\n" +
            "            List<Map<String, Object>> users = query.list();\n" +
            "            resultObject.put(\"objects\", users);\n" +
            "            tx.commit();\n" +
            "\n" +
            "        } catch (Exception e) {\n" +
            "            if (tx != null) {\n" +
            "                tx.rollback();\n" +
            "            }\n" +
            "            throw e;\n" +
            "        }\n" +
            "        resultObject.put(\"success\", true);\n" +
            "        resultObject.put(\"title\", \"取得指定分工中的用户！\");\n" +
            "        resultObject.put(\"message\", \"取得指定分工中的用户。\");\n" +
            "        resultObject.put(\"category\", \"info\");\n" +
            "        response.setContentType(\"application/json\");\n" +
            "        response.setCharacterEncoding(\"utf-8\");\n" +
            "\n" +
            "        ObjectMapper mapper = new ObjectMapper();\n" +
            "        mapper.setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.ALWAYS, JsonInclude.Include.NON_NULL));\n" +
            "        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);\n" +
            "        response.getWriter().write(mapper.writeValueAsString(resultObject));\n" +
            "    }\n" +
            "}"
}

def generateBundle(packageName, out, className, fields) {
    out.println "package $packageName;"
    out.println "\n" +
            "import com.shinow.abc.amili.bundle.Bundle;\n" +
            "import com.shinow.abc.amili.security.PermissionDescriptor;\n" +
            "import com.shinow.abc.${rootModelDir}.*;\n" +
            "\n" +
            "public class ${className}Bundle extends Bundle {\n" +
            "\n" +
            "    @Override\n" +
            "    public String getId() {\n" +
            "        return \"${rootModelDir}\";\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public String getName() {\n" +
            "        return \"xxxxxxxplease_set_NAMExxxxxxx\";\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public void install() {\n" +
            "        super.install();\n" +
            "        this.enroll(new ${className}Action());\n" +
            "        this.enroll(new ${className}sAction());\n" +
            "        this.enroll(new OperatorsAction());\n" +
            "        this.enroll(new ContextMenuAction());\n" +
            "\n" +
            "        PermissionDescriptor readPermissionDescriptor = new PermissionDescriptor(\"${rootModelDir}xxxxxx_read\", \"查看\", \"查看xxxxxxxplease_set_NAMExxxxxxx信息。\");\n" +
            "        this.registPermission(\"xxxxxactionxxxxxx\", readPermissionDescriptor);\n" +
            "        this.registPermission(\"xxxxxactionxxxxxxs\", readPermissionDescriptor);\n" +
            "        this.registPermission(\"operators\", readPermissionDescriptor);\n" +
            "        this.registPermission(\"${rootModelDir}contextmenu\", readPermissionDescriptor);\n" +
            "\n" +
            "        PermissionDescriptor managePermissionDescriptor = new PermissionDescriptor(\"${rootModelDir}_manage\", \"管理\", \"管理xxxxxxxplease_set_NAMExxxxxxx信息。\");\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public void uninstall() {\n" +
            "        super.uninstall();\n" +
            "        this.unenroll(new ${className}Action());\n" +
            "        this.unenroll(new ${className}sAction());\n" +
            "        this.unenroll(new OperatorsAction());\n" +
            "        this.unenroll(new ContextMenuAction());\n" +
            "    }\n" +
            "}"
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