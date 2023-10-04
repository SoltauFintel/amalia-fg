package github.soltaufintel.amalia.fg;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

public class FormularGeneratorMain {
    private int indent = 2;
    private String cancelLink;
    private String template;
    private String putCode;
    private String setCode;
    private boolean edit;
    private boolean mMode = false;
    private String var = "o";
    
    public static void main(String[] args) throws Exception {
        System.out.println("fg - Amalia formular generator\r\n");

        new FormularGeneratorMain().generate(args);
    }

    public void generate(String[] args) throws IOException {
        for (String arg : args) {
            if ("m".equals(arg)) {
                mMode = true;
                continue;
            }
            indent = 2;
            cancelLink = "";
            template = "";
            putCode = "";
            setCode = "";
            edit = false;
            
            File cmd = new File("formular", arg + ".txt");
            System.out.println("** " + cmd.getAbsolutePath());
            if (cmd.isFile()) {
                File target = new File("src/main/resources/templates/" + arg + ".html");
                File target2 = new File("formular/" + arg + "_code.java");
                generate(cmd, target, target2);
            } else {
                System.out.println("   => error: file not found");
            }
            System.out.println();
        }
    }

    public void generate(File cmd, File target, File target2) throws IOException {
        String content = new String(Files.readAllBytes(cmd.toPath()));
        content = content.replace("\r\n", "\n");

        String titel = "Titel";
        String q = cmd.getName().toLowerCase();
        if (q.contains("edit")) {
            titel = "X bearbeiten";
            edit = true;
        } else if (q.contains("add") || q.contains("create")) {
            titel = "Neues X eingeben";
        }
        if (mMode) {
            titel = "{{header}}";
        }

        if (edit) {
            putCode += "    put(\"id\", esc(o.getId()));\r\n";
        }

        template = "{{master: master}}\r\n"//
                + "\r\n"//
                + "<div class=\"row\"> \r\n"//
                + "    <div class=\"col-lg-12\">\r\n"//
                + "        <h1 class=\"page-header\">" + titel + "</h1>\r\n\r\n"//
                + "        <form action=\"[[action]]\" class=\"form-horizontal\" method=\"post\">\r\n"//
                + "            <fieldset>\r\n";

        for (String line : content.split("\n")) {
            if (line.isBlank() || line.trim().startsWith("//")) {
                continue;
            }
            int o = line.indexOf(":");
            String li = line.substring(0, o).trim();
            String re = line.substring(o + 1).trim();
            verteiler(li, re);
        }

        String save = "Speichern";
        String cancel = "Abbruch";
        if (mMode) {
            save = "{{N.save}}";
            cancel = "{{N.cancel}}";
        }
        template += "\r\n";
        template += "                <div class=\"form-group\">\r\n"//
                + "                    <div class=\"col-lg-offset-" + indent + " col-lg-5\">\r\n"//
                + "                        <button type=\"submit\" class=\"btn btn-primary br\">" + save + "</button>\r\n"//
                + "                        <a href=\"" + cancelLink + "\" class=\"btn btn-default\">" + cancel + "</a>\r\n"//
                + "                    </div>\r\n"//
                + "                </div>\r\n"//
                + "            </fieldset>\r\n"//
                + "        </form>\r\n"//
                + "        \r\n"//
                + "    </div>\r\n"//
                + "</div>\r\n";

        try (FileWriter w = new FileWriter(target, Charset.forName("UTF-8"))) {
            w.write(template);
        }
        System.out.println("   written: " + target.getAbsolutePath());
        
        try (FileWriter w = new FileWriter(target2)) {
            w.write(putCode);
            w.write("\r\n");
            w.write(setCode);
        }
        System.out.println("   written: " + target2.getAbsolutePath());
    }

    private void verteiler(String li, String re) {
        switch (li) {
        case "var":
            var = re;
            break;
        case "indent":
            indent = Integer.parseInt(re);
            break;
        case "t":
            textfield(re);
            setCode += "    " + var + ".set" + flu(re) + "(ctx.formParam(\"" + re + "\"));\r\n";
            if (edit) {
                putCode += "    put(\"" + re + "\", esc(" + var + ".get" + flu(re) + "()));\r\n";
            }
            break;
        case "c":
            combobox(re);
            setCode += "    " + var + ".set" + flu(re) + "(ctx.formParam(\"" + re + "\"));\r\n";
            putCode += "    List<String> " + re + "s = new ArrayList<>();\r\n";
            String sel = var + ".get" + flu(re) + "()";
            if (!edit) {
                sel = "\"\"";
            }
            putCode += "    combobox(\"" + re + "s\", " + re + "s, " + sel + ", true, model);\r\n";
            break;
        case "k":
            checkbox(re);
            setCode += "    " + var + ".set" + flu(re) + "(\"on\".equals(ctx.formParam(\"" + re + "\")));\r\n";
            if (edit) {
                putCode += "    put(\"" + re + "\", " + var + ".is" + flu(re) + "());\r\n";
            } else {
                putCode += "    put(\"" + re + "\", false);\r\n";
            }
            break;
        case "ta":
            textarea(re);
            setCode += "    " + var + ".set" + flu(re) + "(ctx.formParam(\"" + re + "\"));\r\n";
            if (edit) {
                putCode += "    put(\"" + re + "\", esc(" + var + ".get" + flu(re) + "()));\r\n";
            }
            break;
        case "e":
            empty();
            break;
        case "version":
            if (edit) {
                template +="                <input type=\"hidden\" name=\"version\" value=\"{{version}}\">\r\n";
                setCode += "    " + var + ".setVersion(Integer.parseInt(ctx.formParam(\"version\")));\r\n";
                putCode += "    putInt(\"version\", " + var + ".getVersion());\r\n";
            }
            break;
        case "ok": // ok link (action)
            template = template.replace("[[action]]", re);
            break;
        case "cancel": // cancel link
            cancelLink = re;
            break;
        default:
            System.err.println("unsupported line: " + li + ": " + re);
        }
    }

    private void textfield(String id) {
      template += "                <div class=\"form-group\">\r\n"//
                + "                    <label for=\"" + id + "\" class=\"col-lg-" + indent + " control-label\">" + flu(id) + "</label>\r\n"//
                + "                    <div class=\"col-lg-3\">\r\n"//
                + "                        <input class=\"form-control\" type=\"text\" id=\"" + id + "\" name=\"" + id + "\"" + (edit ? " value=\"{{" + id + "}}\"" : "") + ">\r\n"//
                + "                    </div>\r\n"//
                + "                </div>\r\n";
    }

    private void textarea(String id) {
      template += "                <div class=\"form-group\">\r\n"//
                + "                    <label for=\"" + id + "\" class=\"col-lg-" + indent + " control-label\">" + flu(id) + "</label>\r\n"//
                + "                    <div class=\"col-lg-3\">\r\n"//
                + "                        <textarea class=\"form-control\" rows=\"4\" id=\"" + id + "\" name=\"" + id + "\">" + (edit ? "{{" + id + "}}" : "") + "</textarea>\r\n"//
                + "                    </div>\r\n"//
                + "                </div>\r\n";
    }

    private void combobox(String id) {
      template += "                <div class=\"form-group\">\r\n"//
                + "                    <label for=\"" + id + "\" class=\"col-lg-" + indent + " control-label\">" + flu(id) + "</label>\r\n"//
                + "                    <div class=\"col-lg-3\">\r\n"//
                + "                        <select class=\"form-control\" id=\"" + id + "\" name=\"" + id + "\">\r\n"//
                + "                            {{each a in " + id + "s}}\r\n"//
                + "                                <option{{if a.selected}} selected{{/if}}>{{a.text}}</option>\r\n"//
                + "                            {{/each}}\r\n"//
                + "                        </select>\r\n"//
                + "                    </div>\r\n"//
                + "                </div>\r\n";
    }

    private void checkbox(String id) {
        String e = "{{if " + id + "}} checked{{/if}}";
      template += "                <div class=\"form-group\">\r\n"//
                + "                    <div class=\"col-lg-3 col-lg-offset-" + indent + "\">\r\n"//
                + "                        <div class=\"checkbox\">\r\n"//
                + "                            <label>\r\n"//
                + "                                <input type=\"checkbox\" id=\"" + id + "\" name=\"" + id + "\"" + (edit ? e : "") + ">\r\n"//
                + "                                " + flu(id) + "\r\n"//
                + "                            </label>\r\n"//
                + "                        </div>\r\n"//
                + "                    </div>\r\n"//
                + "                </div>\r\n";
    }

    private void empty() {
      template += "                <div class=\"form-group\">\r\n"//
                + "                    <label class=\"col-lg-" + indent + " control-label\"></label><div class=\"col-lg-3\"></div>\r\n"//
                + "                </div>\r\n";
    }
    
    private String flu(String s) {
        if (s == null || s.length() <= 2) {
            return s;
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
