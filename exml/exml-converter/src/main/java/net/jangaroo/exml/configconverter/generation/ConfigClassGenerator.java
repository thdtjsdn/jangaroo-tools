package net.jangaroo.exml.configconverter.generation;

import freemarker.core.Environment;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.jangaroo.exml.configconverter.model.ComponentClass;
import net.jangaroo.exml.configconverter.model.ComponentSuite;
import net.jangaroo.exml.configconverter.model.ComponentType;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Locale;

/**
 * Generates new style config classes out of old style annotated AS components.
 */
public final class ConfigClassGenerator {

  private final Logger log = LoggerFactory.getLogger(ConfigClassGenerator.class);

  private final static String outputCharset = "UTF-8";

  private ComponentSuite componentSuite;

  public ConfigClassGenerator(ComponentSuite componentSuite) {
    this.componentSuite = componentSuite;
  }

  public void generateJangarooClass(ComponentClass jooClass, String configClassName, Writer output) throws IOException, TemplateException {
    if (validateComponentClass(jooClass)) {
      Configuration cfg = new Configuration();
      cfg.setClassForTemplateLoading(ComponentClass.class, "/");
      cfg.setObjectWrapper(new DefaultObjectWrapper());
      Template template = cfg.getTemplate("/net/jangaroo/exml/templates/config_class.ftl");
      String type = computeType(jooClass);
      ConfigClassModel configClassModel = new ConfigClassModel(jooClass, componentSuite, configClassName, type);
      log.info("Generate config class '" + configClassModel.getComponentSuite().getConfigClassPackage() + "." + configClassModel.getClassName() + "' for component class '" + jooClass.getFullClassName() + "'");
      Environment env = template.createProcessingEnvironment(configClassModel, output);
      env.setOutputEncoding(outputCharset);
      env.process();
    }
  }

  private String computeType(ComponentClass jooClass) {
    // Try to guess the type of the component. Generally, actions inherit from
    // ext.Action and plugins are named ...Plugin or inherit from such a class.
    // At least, this is a good approximation.
    String type = "xtype";
    ComponentClass currentClass = jooClass;
    while (currentClass != null) {
      if (currentClass.getFullClassName().equals("ext.Action")) {
        type = "";
        break;
      }
      if (currentClass.getClassName().toLowerCase(Locale.ROOT).endsWith("plugin")) {
        type = "ptype";
        break;
      }
      currentClass = currentClass.getSuperClass();
    }
    return type;
  }

  private boolean validateComponentClass(ComponentClass jooClass) {
    boolean isValid = true;

    if (StringUtils.isEmpty(jooClass.getXtype())) {
      log.error(String.format("Xtype of component '%s' is undefined!", jooClass.getFullClassName()));
      isValid = false;
    }

    if (StringUtils.isEmpty(jooClass.getClassName())) {
      log.error(String.format("Class name of component '%s' is undefined!", jooClass.getFullClassName()));
      isValid = false;
    }

    for (String importStr : jooClass.getImports()) {
      if (StringUtils.isEmpty(importStr)) {
        log.error(String.format("An empty import found. Something is wrong in your class %s", jooClass.getFullClassName()));
        isValid = false;
      }
    }

    return isValid;
  }

  public void generateClasses() {
    for (ComponentClass cc : componentSuite.getComponentClassesByType(ComponentType.ActionScript)) {
      generateClass(cc);
    }
  }

  public File generateClass(ComponentClass componentClass) {
    File configClassDir = new File(componentSuite.getAs3OutputDir(), componentSuite.getConfigClassPackage().replace('.', File.separatorChar));
    String configClassName = StringUtils.uncapitalise(componentClass.getLastXtypeComponent());
    File outputFile = new File(configClassDir, configClassName + ".as");

    if (!outputFile.getParentFile().exists()) {
      if (outputFile.getParentFile().mkdirs()) {
        log.debug("Folder '" + outputFile.getParentFile().getAbsolutePath() + "' created.");
      }
    }

    Writer writer = null;
    try {
      writer = new OutputStreamWriter(new FileOutputStream(outputFile), outputCharset);
      generateJangarooClass(componentClass, configClassName, writer);
    } catch (IOException e) {
      log.error("Exception while creating class", e);
    } catch (TemplateException e) {
      log.error("Exception while creating class", e);
    } finally {
      try {
        if (writer != null) {
          writer.close();
        }
      } catch (IOException e) {
        //never happen
      }
    }
    return outputFile;
  }
}
