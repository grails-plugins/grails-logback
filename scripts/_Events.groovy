// rebuilds the jar file to include source
eventPackagePluginEnd = {
	if (!(it instanceof Binding)) {
		return
	}

	String repackage = 'target/repackage'
	String jarPath = "target/grails-plugin-logback-${pluginInfo.version}.jar"

	ant.delete dir: repackage
	ant.unzip src: jarPath, dest: repackage

	String src = "$repackage/src"

	ant.mkdir dir: src

	ant.copy(todir: src) {
		fileset dir: "src", {
			include name: "**/*.groovy"
			include name: "**/*.java"
		}
	}

	ant.copy file: 'LogbackGrailsPlugin.groovy', todir: src

	ant.zip destfile: jarPath, basedir: repackage
}
