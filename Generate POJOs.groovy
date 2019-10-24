import com.intellij.database.model.DasTable
import com.intellij.database.model.ObjectKind
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

packageName = "package models"
typeMapping = [
  (~/(?i)int|int4/)                      : "int",
  (~/(?i)float|double|decimal|real/): "double",
  (~/(?i)datetime|timestamp/)       : "time.Time",
  (~/(?i)date/)                     : "time.Date",
  (~/(?i)time/)                     : "time.Time",
  (~/(?i)/)                         : "string"
]

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
  SELECTION.filter { it instanceof DasTable && it.getKind() == ObjectKind.TABLE }.each { generate(it, dir) }
}

def generate(table, dir) {
  def className = javaName(table.getName(), true)
  def fields = calcFields(table)
  new File(dir, className + ".go").withPrintWriter { out -> generate(out, className, fields) }
}

def generate(out, className, fields) {
  out.println "package $packageName"
  out.println ""
  out.println "// make from haiyang.sun.2011@gmail"
  out.println "type $className struct {"
  fields.each() {
    if (it.annos != "") out.println "  ${it.annos}"
    out.println "  ${it.name} ${it.type};"
  }
   out.println "}"

  out.println ""
  fields.each() {
    out.println ""
    out.println "func (bean *$className)   Get${it.name.capitalize()}() ${it.type}{"
    out.println "    return bean.${it.name};"
    out.println "}"
    out.println ""
    out.println "func (bean *$className)   Set${it.name.capitalize()}(${it.name} ${it.type}) {"
    out.println "    bean.${it.name} = ${it.name};"
    out.println "}"
    out.println ""
  }

}

def calcFields(table) {
  DasUtil.getColumns(table).reduce([]) { fields, col ->
    def spec = Case.LOWER.apply(col.getDataType().getSpecification())
    def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
    fields += [[
                 name : javaName(col.getName(), true),
                 type : typeStr,
                 annos: ""]]
  }
}

def javaName(str, capitalize) {
  def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
    .collect { Case.LOWER.apply(it).capitalize() }
    .join("")
    .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
  capitalize || s.length() == 1? s : Case.LOWER.apply(s[0]) + s[1..-1]
}
