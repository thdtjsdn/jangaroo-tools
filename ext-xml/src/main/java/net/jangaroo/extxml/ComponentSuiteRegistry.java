package net.jangaroo.extxml;

import java.util.Map;
import java.util.LinkedHashMap;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;

/**
 * A registry for ComponentSuites, which returns a ComponentSuite given its namespace URI.
 * It the ComponentSuite is not yet registered, it uses a ComponentSuiteResolver to construct it and
 * caches it for later access.
 * This class, more precise method {@link #getComponentSuite}, is not thread-safe!
 */
public class ComponentSuiteRegistry {

  private static final ComponentSuiteResolver NO_COMPONENT_SUITE_RESOLVER =   new ComponentSuiteResolver() {
    public InputStream resolveComponentSuite(String namespaceUri) throws IOException {
      throw new IOException("No ComponentSuiteResolver installed.");
    }
  };

  private final Map<String, ComponentSuite> componentSuitesByNamespaceUri = new LinkedHashMap<String, ComponentSuite>(10);
  private final ComponentSuiteResolver componentSuiteResolver;
  private ErrorHandler errorHandler = new StandardOutErrorHandler();
  public static final String EXT_JS_3_NAMESPACE_URI = "http://extjs.com/ext3";

  public ComponentSuiteRegistry() {
    this(NO_COMPONENT_SUITE_RESOLVER);
  }

  public ComponentSuiteRegistry(ComponentSuiteResolver componentSuiteResolver) {
    this.componentSuiteResolver = componentSuiteResolver;
  }

  private InputStream getExt3XsdInputStream() throws IOException {
    URL jarUrl = getClass().getResource("/net/jangaroo/extxml/schemas/ext3.xsd");
    return jarUrl != null ? jarUrl.openStream() : null;
  }

  public void setErrorHandler(ErrorHandler errorHandler) {
    this.errorHandler = errorHandler;
  }

  public void add(ComponentSuite componentSuite) {
    componentSuitesByNamespaceUri.put(componentSuite.getNamespace(), componentSuite);
  }

  public ComponentSuite getComponentSuite(String namespaceUri) {
    ComponentSuite componentSuite = componentSuitesByNamespaceUri.get(namespaceUri);
    if (componentSuite == null) {
      try {
        InputStream xsdInputStream =
          EXT_JS_3_NAMESPACE_URI.equals(namespaceUri)
            ? getExt3XsdInputStream()
            : componentSuiteResolver.resolveComponentSuite(namespaceUri);
        if (xsdInputStream == null) {
          errorHandler.error("No XSD registered for namespace URI " + namespaceUri);
        } else {
          componentSuite = new XsdScanner(errorHandler).scan(xsdInputStream);
          assert namespaceUri.equals(componentSuite.getNamespace());
          add(componentSuite);
        }
      } catch (IOException e) {
        errorHandler.error("Could not resolve XSD for namespace URI " + namespaceUri, e);
      }
    }
    return componentSuite;
  }

  public ComponentClass findComponentClassByXtype(String xtype) {
    // make sure Ext 3 standard components are loaded:
    getComponentSuite(EXT_JS_3_NAMESPACE_URI);
    // now, search all known component suites for xtype:
    for (ComponentSuite suite : componentSuitesByNamespaceUri.values()) {
      ComponentClass componentClass = suite.getComponentClassByXtype(xtype);
      if (componentClass != null) {
        return componentClass;
      }
    }
    return null;
  }

  public ComponentClass findComponentClassByFullClassName(String fullComponentClassName) {
    // make sure Ext 3 standard components are loaded:
    getComponentSuite(EXT_JS_3_NAMESPACE_URI);
    // now, search all known component suites for fully qualified component class name:
    for (ComponentSuite suite : componentSuitesByNamespaceUri.values()) {
      ComponentClass componentClass = suite.getComponentClassByFullClassName(fullComponentClassName);
      if (componentClass != null) {
        return componentClass;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (ComponentSuite suite : componentSuitesByNamespaceUri.values()) {
      sb.append(suite.getNamespace()).append(", ");
    }
    return sb.toString();
  }
}