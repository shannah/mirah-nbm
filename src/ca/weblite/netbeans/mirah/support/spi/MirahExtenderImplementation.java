/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */

package ca.weblite.netbeans.mirah.support.spi;

import org.netbeans.api.project.Project;
import ca.weblite.netbeans.mirah.support.api.MirahExtender;

/**
 * SPI for activation/deactivation of Mirah support in certain {@link Project}.
 * This enables to change build script in Ant based projects, <i>pom.xml</i> in Maven based projects etc.
 *
 * <p>
 * Projects can provide implementation of this interface in its {@link Project#getLookup lookup}
 * to allow clients to activate/deactivate Mirah support or to find out if the support is
 * already active for a certain {@link Project}.
 *
 * @author Martin Janicek <mjanicek@netbeans.org>
 *
 * @see MirahExtender
 * @since 1.22
 */
public interface MirahExtenderImplementation {

    /**
     * Check if mirah has been already activated for the project.
     *
     * @return {@code true} if the Mirah support is already active,
     *         {@code false} if the Mirah support is not active yet
     */
    public boolean isActive();

    /**
     * Activates Mirah support for the project. (e.g. when new Mirah file
     * is created). Implementator should change project configuration with respect
     * to mirah source files (e.g. change the ant build script and use mirahc
     * instead of javac, update pom.xml in maven etc.)
     *
     * @return {@code true} if activation were successful, {@code false} otherwise
     */
    public boolean activate();

    /**
     * Called when mirah is deactivated for a certain project. This is an inverse
     * action to the {@code activate} method. Implementator should make opposite steps
     * in the project configuration (e.g. remove maven-mirah-plugin and related mirah
     * dependencies from pom.xml, change the ant build script to use javac again etc.)
     *
     * @return {@code true} if deactivation were successful, {@code false} otherwise
     */
    public boolean deactivate();
    
    public boolean isCurrent();
    public boolean update();
    
}
