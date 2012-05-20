/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 Pavlo Baron
 * mailto:pb AT pbit DOT org
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.java.whatif;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.resources.InputFile;
import org.sonar.java.ast.CheckstyleSquidBridge;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FullIdent;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

public class WhatifPackageRefactoring extends WhatifRefactoring {

    private static final Logger LOG = LoggerFactory.getLogger(WhatifPackageRefactoring.class);
    
    @Override
    public void refactor(DetailAST ast, InputFile inputFile) {
        if (ast != null) {
            if (ast.getType() == TokenTypes.PACKAGE_DEF) {
                String p = FullIdent.createFullIdent(ast.getLastChild().getPreviousSibling()).getText();
                String prop = WhatifRefactoring.getProps().getProperty(p);
                //TODO: case a.b.c.* or even more asterisks
                if (prop != null) {
                    LOG.info("Refactoring package \"" + p + "\" to \"" + prop + "\"");
                    
                    DetailAST a = ast.getLastChild().getPreviousSibling();
                    
                    //ast.getLastChild().getPreviousSibling().setText(prop);
                    
                    a.getFirstChild().getNextSibling().setText("b");
                    
                    String newPath = inputFile.getRelativePath();
                    newPath = newPath.replaceFirst(p.replace('.', '/'), prop.replace('.', '/'));
                    inputFile.setRelativePath(newPath);
                    CheckstyleSquidBridge.correctInputFile(inputFile);
                    
                    if (ast != null);    
                }
            }
        }
    }
}
