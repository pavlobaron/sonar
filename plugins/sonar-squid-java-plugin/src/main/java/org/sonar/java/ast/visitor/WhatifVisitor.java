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

package org.sonar.java.ast.visitor;

import org.sonar.java.whatif.WhatifPackageRefactoring;
import org.sonar.squid.indexer.SquidIndex;

import com.puppycrawl.tools.checkstyle.api.DetailAST;

public class WhatifVisitor extends JavaAstVisitor {
    
    public WhatifVisitor(SquidIndex indexer) {
    }

    @Override
    public void visitFile(DetailAST ast) {
        (new WhatifPackageRefactoring()).refactor(ast, getInputFile());
    }
}
