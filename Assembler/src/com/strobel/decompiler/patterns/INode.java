/*
 * INode.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.decompiler.patterns;

public interface INode {
    boolean isNull();
    Role getRole();
    INode getFirstChild();
    INode getNextSibling();

    boolean matches(final INode other, final Match match);
    boolean matchesCollection(final Role role, final INode position, final Match match, final BacktrackingInfo backtrackingInfo);
    Match match(INode other);
    boolean matches(INode other);
}
