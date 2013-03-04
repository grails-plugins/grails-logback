grails.project.work.dir = 'target'
grails.project.source.level = 1.6
grails.project.target.level = 1.6
grails.project.docs.output.dir = 'docs/manual' // for backwards-compatibility, the docs are checked into gh-pages branch

grails.project.dependency.resolution = {

	inherits 'global', {
		excludes 'grails-plugin-log4j'
	}

	log 'warn'
	checksums true

	repositories {
		inherits true
		grailsCentral()
		mavenLocal()
		mavenCentral()
	}

	dependencies {
		compile 'ch.qos.logback:logback-classic:1.0.9', {
			excludes 'dom4j', 'fest-assert', 'geronimo-jms_1.1_spec', 'greenmail','groovy-all', 'h2',
			         'hsqldb', 'integration', 'janino', 'junit', 'log4j-over-slf4j', 'logback-core',
			         'mail', 'mysql-connector-java', 'org.apache.felix.main', 'postgresql', 'servlet-api',
			         'scala-library', 'slf4j-api', 'slf4j-ext', 'subethasmtp'
		}
		compile 'ch.qos.logback:logback-core:1.0.9', {
			excludes 'janino', 'jansi', 'mail', 'geronimo-jms_1.1_spec', 'easymock', 'servlet-api', 'scala-library', 'junit', 'fest-assert'
		}

		test 'javax.mail:mail:1.4.5', {
			transitive = false
			export = false
		}
	}

	plugins {
		build(':release:2.2.0', ':rest-client-builder:1.0.3') {
			export = false
		}

		test ':dumbster:0.2', {
			export = false
		}
	}
}
