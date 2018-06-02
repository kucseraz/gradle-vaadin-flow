/*
 * Copyright 2018 Devsoap Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.devsoap.vaadinflow.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction

/**
 * Generates corresponding HTML files for CSS files located in frontend/styles
 *
 * @author John Ahlroos
 * @since 1.0
 */
@CacheableTask
class ConvertCssToHtmlStyleTask extends DefaultTask {

    static final String NAME = 'vaadinConvertStyleCssToHtml'

    static final String STYLES_PATH = 'src/main/webapp/frontend/styles'
    static final String STYLES_TARGET_PATH = 'build/webapp-gen/frontend/styles'

    @InputFiles
    FileTree cssFiles = project.fileTree(STYLES_PATH).matching { it.include( '**/*.css') }

    @OutputDirectory
    File targetPath = project.file(STYLES_TARGET_PATH)

    @OutputFiles
    FileTree htmlFiles = project.fileTree(STYLES_TARGET_PATH).matching { it.include( '**/*.html') }

    @TaskAction
    void run() {
        cssFiles.each {
            String content = """
            <!-- This is a autogenerated html file for ${it.name}. Do not edit this file, it will be overwritten. -->
            <custom-style><style>
            """.stripIndent()

            content += it.text

            content += '\n</style></custom-style>'

            project.file("$STYLES_TARGET_PATH/${it.name - '.css'}.html").text = content
        }
    }
}