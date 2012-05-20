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

                    correctAST(ast, prop);
                    correctInputFile(inputFile, p, prop);
                }
            }
        }
    }
    
    protected DetailAST doToken(DetailAST curr, String[] tokens, int pos) {
        DetailAST astNew = new DetailAST();
        DetailAST astR = new DetailAST();
        if (pos == 0) {
            astNew.initialize(TokenTypes.IDENT, tokens[0]);
            astR.initialize(TokenTypes.IDENT, tokens[1]);
            astNew.setNextSibling(astR);
            
            return doToken(astNew, tokens, 2);
        } else if (pos < tokens.length) {
            astNew.initialize(TokenTypes.DOT, ".");
            astR.initialize(TokenTypes.IDENT, tokens[pos]);
            astNew.setNextSibling(astR);
            astNew.addChild(curr);
            
            return doToken(astNew, tokens, pos + 1);
        } else {
            astNew.initialize(TokenTypes.DOT, ".");
            astR.initialize(TokenTypes.SEMI, ";");
            astNew.setNextSibling(astR);
            astNew.addChild(curr);
            
            return astNew;
        }
        
    }
    
    protected void correctAST(DetailAST ast, String pac) {
        String[] tokens = pac.split("\\.");
        DetailAST anno = ast.getFirstChild();
        
        anno.setNextSibling(doToken(null, tokens, 0));
    }
    
    protected void correctInputFile(InputFile inputFile, String oldPackage, String newPackage) {
        String newPath = inputFile.getRelativePath();
        newPath = newPath.replaceFirst(
                oldPackage.replace('.', '/'), newPackage.replace('.', '/'));
        inputFile.setRelativePath(newPath);
        CheckstyleSquidBridge.correctInputFile(inputFile);
    }
}
