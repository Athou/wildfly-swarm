package org.wildfly.swarm.container.runtime.xmlconfig;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.namespace.QName;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ProfileParsingCompletionHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.staxmapper.XMLElementReader;
import org.wildfly.swarm.internal.SwarmMessages;
import org.wildfly.swarm.spi.api.Fraction;
import org.wildfly.swarm.spi.api.annotations.WildFlyExtension;

/**
 * @author Bob McWhirter
 */
@Singleton
public class StandaloneXMLParserProducer {

    @Inject
    private Instance<Fraction> fractions;

    private StandaloneXMLParser parser = new StandaloneXMLParser();

    @PostConstruct
    public void setupFactories() {
        this.fractions.forEach(this::setupFactory);
    }

    @Produces
    @Singleton
    StandaloneXMLParser standaloneXmlParser() {
        return this.parser;
    }

    private void setupFactory(Fraction fraction) {
        WildFlyExtension anno = fraction.getClass().getAnnotation(WildFlyExtension.class);

        if ( anno == null ) {
            return;
        }

        String extensionModuleName = anno.module();
        String extensionClassName = anno.classname();
        boolean noClass = anno.noClass();

        if (extensionClassName != null && extensionClassName.trim().isEmpty()) {
            extensionClassName = null;
        }

        try {
            Module extensionModule = Module.getBootModuleLoader().loadModule(ModuleIdentifier.create(extensionModuleName));

            if (noClass) {
                // ignore it all
            } else if (extensionClassName != null) {
                Class<?> extCls = extensionModule.getClassLoader().loadClass(extensionClassName);
                try {
                    Extension ext = (Extension) extCls.newInstance();
                    add(ext);
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            } else {
                ServiceLoader<Extension> extensionLoader = extensionModule.loadService(Extension.class);

                Iterator<Extension> extensionIter = extensionLoader.iterator();
                List<Extension> extensions = new ArrayList<>();

                if (extensionIter.hasNext()) {
                    Extension ext = extensionIter.next();
                    extensions.add(ext);
                }

                if (extensions.size() > 1) {
                    throw SwarmMessages.MESSAGES.fractionHasMultipleExtensions(fraction.getClass().getName(), extensions);
                }

                if (!extensions.isEmpty()) {
                    add(extensions.get(0));
                }
            }
        } catch (ModuleLoadException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void add(Extension ext) {
        ParsingContext ctx = new ParsingContext();
        ext.initializeParsers(ctx);
    }

    class ParsingContext implements ExtensionParsingContext {
        @Override
        public ProcessType getProcessType() {
            return ProcessType.STANDALONE_SERVER;
        }

        @Override
        public RunningMode getRunningMode() {
            return RunningMode.NORMAL;
        }

        @Override
        public void setSubsystemXmlMapping(String localName, String namespace, XMLElementReader<List<ModelNode>> parser) {
            StandaloneXMLParserProducer.this.parser.addDelegate( new QName( namespace, "subsystem"), parser );
        }

        @Override
        public void setProfileParsingCompletionHandler(ProfileParsingCompletionHandler profileParsingCompletionHandler) {
            // ignore
        }
    }

}
