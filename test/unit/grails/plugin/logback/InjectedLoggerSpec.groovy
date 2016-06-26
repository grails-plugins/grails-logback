package grails.plugin.logback

import grails.test.mixin.*
import groovy.util.logging.Log
import groovy.util.logging.Slf4j

import java.lang.reflect.Field
import java.lang.reflect.Modifier

import spock.lang.Specification

@SuppressWarnings('unused')
class InjectedLoggerSpec extends Specification {

	void 'no conflict'() {
		when:
		Collection<Field> instanceFields = fields(Simple, false)
		Collection<Field> staticFields = fields(Simple, true)

		then:
		instanceFields.size() == 1
		instanceFields[0].type == String

		staticFields.size() == 1
		assertLogbackLogger staticFields[0]
	}

	void 'no Logger field is added when there is an existing "log" field that is private and static'() {
		when:
		Collection<Field> instanceFields = fields(HasStaticField, false)
		Collection<Field> staticFields = fields(HasStaticField, true)

		then:
		instanceFields.size() == 1
		instanceFields[0].type == int

		staticFields.size() == 1
		staticFields[0].type == String
	}

	void 'an existing private and static Logger field not named "log" results in two Logger fields'() {
		when:
		Collection<Field> instanceFields = fields(HasStaticFieldDifferentName, false)
		Collection<Field> staticFields = fields(HasStaticFieldDifferentName, true)

		then:
		instanceFields.size() == 1
		instanceFields[0].type == double

		staticFields.size() == 2
		assertLogbackLogger staticFields[0]
		assertLogbackLogger staticFields[1], 'logger'
	}

	void 'the Slf4j annotation creates the same field that the plugin would have'() {
		when:
		Collection<Field> instanceFields = fields(Annotated, false)
		Collection<Field> staticFields = fields(Annotated, true)

		then:
		instanceFields.size() == 1
		instanceFields[0].type == long

		staticFields.size() == 1
		assertLogbackLogger staticFields[0]
	}

	void 'the Slf4j annotation with a non-default name results in two Logger fields'() {
		when:
		Collection<Field> instanceFields = fields(AnnotatedDifferentName, false)
		Collection<Field> staticFields = fields(AnnotatedDifferentName, true)

		then:
		instanceFields.size() == 1
		instanceFields[0].type == boolean

		staticFields.size() == 2
		assertLogbackLogger staticFields[0]
		assertLogbackLogger staticFields[1], 'logger'
	}

	void 'the Log annotation creates the same field as the plugin but the type is java.util.logging.Logger'() {
		when:
		Collection<Field> instanceFields = fields(AnnotatedOtherType, false)
		Collection<Field> staticFields = fields(AnnotatedOtherType, true)

		then:
		instanceFields.size() == 1
		instanceFields[0].type == short

		staticFields.size() == 1
		staticFields[0].type == java.util.logging.Logger
	}

	void 'a non-static "log" field prevents injection of a Logger field'() {
		when:
		Collection<Field> instanceFields = fields(HasInstanceField, false)
		Collection<Field> staticFields = fields(HasInstanceField, true)

		then:
		instanceFields.size() == 2
		instanceFields[0].type == byte
		instanceFields[1].type == String

		!staticFields
	}

	private boolean assertLogbackLogger(Field field, String name = 'log') {
		assert field.name == name
		assert field.type == org.slf4j.Logger
		assert isStatic(field)
		assert isPrivate(field)
		true
	}

	private List<Field> fields(Class c, boolean onlyStatic) {
		fields(c).findAll { onlyStatic == isStatic(it) }.sort { it.name } as List
	}

	private Collection<Field> fields(Class c) {
		//excludes these common fields added by Groovy:
		//  private static org.codehaus.groovy.reflection.ClassInfo $staticClassInfo
		//  public static transient boolean __$stMC
		//  private transient groovy.lang.MetaClass metaClass
		//  private static java.lang.ref.SoftReference $callSiteArray

		Collection<Field> fields = []
		for (Field f in c.declaredFields) {
			if (f.name.startsWith('$')) continue
			if (f.name == '__$stMC') continue
			if (f.name == 'metaClass' && isPrivate(f) && !isStatic(f)) continue
			fields << f
		}
		fields
	}

	private boolean isPrivate(Field field) {
		Modifier.isPrivate field.modifiers
	}

	private boolean isStatic(Field field) {
		Modifier.isStatic field.modifiers
	}
}

class Simple {
	String s
}

class HasStaticField {
	int c
	private static String log
}

class HasStaticFieldDifferentName {
	double d
	private static org.slf4j.Logger logger
}

@Slf4j
class Annotated {
	long l
}

@Slf4j('logger')
class AnnotatedDifferentName {
	boolean b
}

@Log
class AnnotatedOtherType {
	short s
}

class HasInstanceField {
	byte b
	private String log
}
