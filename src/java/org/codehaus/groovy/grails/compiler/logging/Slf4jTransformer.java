package org.codehaus.groovy.grails.compiler.logging;

import java.lang.reflect.Modifier;
import java.net.URL;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.compiler.injection.AllArtefactClassInjector;
import org.codehaus.groovy.grails.compiler.injection.AstTransformer;
import org.codehaus.groovy.grails.compiler.injection.ClassInjector;
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds an Slf4j log field to all artifacts. Note must stay in
 * org.codehaus.groovy.grails.compiler or below to be discovered.
 * Based on org.codehaus.groovy.grails.compiler.logging.LoggingTransformer.
 *
 * @author Graeme Rocher
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
@AstTransformer
public class Slf4jTransformer implements AllArtefactClassInjector, Comparable<ClassInjector> {

	public void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode) {
		final FieldNode existingField = classNode.getDeclaredField("log");
		if (existingField != null || classNode.isInterface()) {
			return;
		}

		final String path = source.getName();
		String artefactType = path == null ? null : GrailsResourceUtils.getArtefactDirectory(path);

		// little bit of a hack, since filters aren't kept in a
		// grails-app/filters directory as they probably should be
		if (artefactType != null && "conf".equals(artefactType) && classNode.getName().endsWith("Filters")) {
			artefactType = "filters";
		}

		String logName = artefactType == null ? classNode.getName() : "grails.app." + artefactType + "." + classNode.getName();

		FieldNode logVariable = new FieldNode("log", Modifier.STATIC | Modifier.PRIVATE, new ClassNode(Logger.class),
				classNode, new MethodCallExpression(new ClassExpression(new ClassNode(LoggerFactory.class)), "getLogger",
						new ArgumentListExpression(new ConstantExpression(logName))));
		classNode.addField(logVariable);
	}

	public void performInjection(SourceUnit source, ClassNode classNode) {
		performInjection(source, null, classNode);
	}

	public void performInjectionOnAnnotatedClass(SourceUnit source, ClassNode classNode) {
		performInjection(source, classNode);
	}

	public boolean shouldInject(URL url) {
		return true; // Add log property to all artifact types
	}

	public int compareTo(ClassInjector other) {
		// ensure this runs before LoggingTransformer which adds a commons-logging logger
		return -1;
	}
}
