/*
Stratagem is a model checker for transition systems described using rewriting
rules and strategies.
Copyright (C) 2013 - SMV@Geneva University.
Program written by Edmundo Lopez Bobeda <edmundo [at] lopezbobeda.net>.
This program is free software; you can redistribute it and/or modify
it under the  terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package ch.unige.cui.smv.stratagem.sigmadd.rewriters

import ch.unige.cui.smv.stratagem.sigmadd.SigmaDDFactoryImpl

/**
 * Implements the union strategy.
 * @param rewriter1 represents one strategy (in rewriter form) of the original union strategy.
 * @param rewriter2 represents one strategy (in rewriter form) of the original union strategy.
 */
case class UnionRewriter(rewriter1: SigmaDDRewriter, rewriter2: SigmaDDRewriter, override val sigmaDDFactory: SigmaDDFactoryImpl) extends SigmaDDRewriter(sigmaDDFactory) {

  override lazy val hashCode = (this.getClass(), rewriter1, rewriter2).hashCode

  override lazy val toString = (new StringBuilder("UnionRewriter(") append rewriter1.toString append ", " append rewriter2.toString append ")").toString

  override def equals(obj: Any): Boolean = obj match {
    case that @ UnionRewriter(r1, r2, _) => (this eq that) || ((rewriter1 == r1) && (rewriter2 == r2))
    case _ => false
  }

  def apply(sigmaDD: SigmaDDImplType): Option[SigmaDDImplType] = rewriter1(sigmaDD) match {
    case None => None
    case Some(result1) => rewriter2(sigmaDD) match {
      case None => None
      case Some(result2) => Some(result1 v result2)
    }
  }
}