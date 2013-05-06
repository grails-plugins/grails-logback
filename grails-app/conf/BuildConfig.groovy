grails.project.work.dir = 'target'
grails.project.docs.output.dir = 'docs/manual' // for backwards-compatibility, the docs are checked into gh-pages branch

grails.project.dependency.resolution = {

	inherits 'global', {
		excludes 'grails-plugin-log4j'
	}

	log 'warn'
	checksums true

	repositories {
		grailsCentral()
		mavenLocal()
		mavenCentral()
	}

	String logbackVersion = '1.0.12'

	dependencies {
		compile "ch.qos.logback:logback-classic:$logbackVersion", {
			excludes 'dom4j', 'fest-assert', 'geronimo-jms_1.1_spec', 'greenmail', 'groovy-all', 'h2',
			         'hsqldb', 'integration', 'janino', 'junit', 'log4j-over-slf4j', 'logback-core',
			         'mail', 'mysql-connector-java', 'org.apache.felix.main', 'postgresql', 'scala-library',
			         'servlet-api', 'slf4j-api', 'slf4j-ext', 'subethasmtp'
		}
		compile "ch.qos.logback:logback-core:$logbackVersion", {
			excludes 'easymock', 'fest-assert', 'geronimo-jms_1.1_spec', 'janino', 'jansi', 'junit',
			         'mail', 'scala-library', 'servlet-api'
		}
		test 'javax.mail:mail:1.4.5', {
			transitive = false
			export = false
		}
	}

	plugins {
		build(':release:2.2.1', ':rest-client-builder:1.0.3') {
			export = false
		}

		test ':dumbster:0.2', {
			export = false
		}
	}
}
