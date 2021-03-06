// Copyright 2014 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.lib.syntax;

import com.google.common.annotations.VisibleForTesting;
import com.google.devtools.build.lib.events.Location;
import java.io.IOException;
import java.util.List;

/** An Node is a node in a Starlark syntax tree. */
public abstract class Node {

  private Location location;

  protected Node() {}

  /**
   * Returns whether this node represents a new scope, e.g. a function call.
   */
  protected boolean isNewScope()  {
    return false;
  }

  @VisibleForTesting  // productionVisibility = Visibility.PACKAGE_PRIVATE
  public void setLocation(Location location) {
    this.location = location;
  }

  /** @return the same node with its location set, in a slightly more fluent style */
  public static <NodeT extends Node> NodeT setLocation(Location location, NodeT node) {
    node.setLocation(location);
    return node;
  }

  public Location getLocation() {
    return location;
  }

  /** Number of spaces that each indentation level expands to when pretty-printing. */
  public static final int INDENT_WIDTH = 2;

  /** Writes out the indentation prefix for a line. */
  protected void printIndent(Appendable buffer, int indentLevel) throws IOException {
    for (int i = 0; i < indentLevel * INDENT_WIDTH; i++) {
      buffer.append(' ');
    }
  }

  /**
   * Writes out a suite of statements. The statements are indented one more level than given, i.e.,
   * the {@code indentLevel} parameter should be the same as the parent node's.
   *
   * <p>This also prints out a {@code pass} line if the suite is empty.
   */
  protected void printSuite(Appendable buffer, List<Statement> statements, int parentIndentLevel)
      throws IOException {
    if (statements.isEmpty()) {
      printIndent(buffer, parentIndentLevel + 1);
      buffer.append("pass\n");
    } else {
      for (Statement stmt : statements) {
        stmt.prettyPrint(buffer, parentIndentLevel + 1);
      }
    }
  }

  /**
   * Writes a pretty-printed representation of this node to a buffer, assuming the given starting
   * indentation level.
   *
   * <p>For expressions, the indentation level is ignored. For statements, the indentation is
   * written, then the statement contents (which may include multiple lines with their own
   * indentation), then a newline character.
   *
   * <p>Indentation expands to {@code INDENT_WIDTH} many spaces per indent.
   *
   * <p>Pretty printing returns the canonical source code corresponding to an AST. Generally, the
   * output can be round-tripped: Pretty printing an AST and then parsing the result should give you
   * back an equivalent AST.
   *
   * <p>Pretty printing can also be used as a proxy for comparing for equality between two ASTs.
   * This can be very useful in tests. However, it is still possible for two different trees to have
   * the same pretty printing. In particular, {@link BuildFileAST} includes import metadata and
   * comment information that is not reflected in the string.
   */
  public abstract void prettyPrint(Appendable buffer, int indentLevel) throws IOException;

  /** Same as {@link #prettyPrint(Appendable, int)}, except with no indent. */
  public void prettyPrint(Appendable buffer) throws IOException {
    prettyPrint(buffer, 0);
  }

  /** Returns a pretty-printed representation of this node. */
  public String prettyPrint() {
    StringBuilder builder = new StringBuilder();
    try {
      prettyPrint(builder);
    } catch (IOException e) {
      // Not possible for StringBuilder.
      throw new AssertionError(e);
    }
    return builder.toString();
  }

  /**
   * Print the syntax node in a form useful for debugging.
   *
   * <p>The output is not precisely specified; use {@link #prettyPrint()} if you need more stable
   * and complete information. For instance, this function may omit child statements of compound
   * statements, or parentheses around some expressions. It may also abbreviate large list literals.
   */
  @Override
  public String toString() {
    return prettyPrint();
  }

  /**
   * Implements the double dispatch by calling into the node specific <code>visit</code> method of
   * the {@link NodeVisitor}
   *
   * @param visitor the {@link NodeVisitor} instance to dispatch to.
   */
  public abstract void accept(NodeVisitor visitor);
}
