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

package ch.unige.cui.smv.stratagem.sigmadd

import scala.collection.immutable.HashMap
import ch.unige.cui.smv.stratagem.ipf.IPFAbstractFactory
import ch.unige.cui.smv.stratagem.util.OperationCache
import ch.unige.cui.smv.stratagem.util.Element
import scala.language.postfixOps
import ch.unige.cui.smv.stratagem.util.OperationCache
import ch.unige.smv.cui.metamodel.adt.ATerm

/**
 * This factory represents the tail of a SigmaDD.
 */
class SigmaDDInductiveIPFFactoryImpl(val sigmaddFactory: SigmaDDFactoryImpl) extends IPFAbstractFactory {

  type CanonicalType = InductiveIPFImpl

  type SigmaDDType = SigmaDDFactoryImpl#SigmaDDImpl

  type FromType = Map[SigmaDDType, SigmaDDInductiveIPFFactoryImpl#InductiveIPFImpl]

  protected def makeFrom(alpha: AnyRef): InductiveIPFImpl = alpha match {
    case a: HashMap[SigmaDDType, InductiveIPFImpl] @unchecked => new InductiveIPFImpl(a) with OperationCache
    case _ => throw new IllegalArgumentException("Unable to create IPF")
  }

  /**
   * Creates an inductive IPF from a list of terms.
   *
   * @param terms the list of terms
   * @return the inductive IPF representing that set of terms n-ary relations.
   */
  def create(terms: List[ATerm]): InductiveIPFImpl = terms match {
    case Nil => TopIPF
    case x :: tail => create(HashMap(sigmaddFactory.create(x) -> create(tail)))
  }

  /**
   * Instanciate a list of terms with a set of variables.
   *
   * @param terms the list of terms
   * @param variables the map containing the values for the instanciation of the variables.
   * @return the inductive IPF representing that set of terms n-ary relations.
   */
  def instanciate(terms: List[ATerm], variables: Map[String, SigmaDDType]): InductiveIPFImpl = terms match {
    case Nil => TopIPF
    case x :: tail => create(HashMap(sigmaddFactory.instantiate(x, variables) -> instanciate(tail, variables)))
  }

  /**
   * This class implements an inductive injective partitioned function. It is
   * meant to encode n-ary relation of terms of a SigmaDD, i.e. the set of
   * arguments.
   */
  private[sigmadd] class InductiveIPFImpl private[sigmadd] (
    val alpha: Map[SigmaDDType, InductiveIPFImpl]) extends IPF {
    type LatticeElementType = InductiveIPFImpl
    type DomainType = SigmaDDType
    type ImageType = InductiveIPFImpl

    type DomainTypeElt = String
    type ImageTypeElt = String

    def wrapped = createWrapped(this)

    def listOfSubtermsVector: List[List[String]] = for (
      sigmaDD <- alpha.keys.toList;
      term <- sigmaDD.listOfTermsAsString;
      listOfSubSequentTerm <- alpha(sigmaDD).listOfSubtermsVector
    ) yield term :: listOfSubSequentTerm

    override lazy val hashCode = alpha##

    lazy val size: BigInt = alpha.map((e) => e._1.size * e._2.size).reduce(_ + _)

    def asBinaryRelation: Set[(DomainTypeElt, ImageTypeElt)] = throw new NotImplementedError

    def asElements: Element = alpha.map((e) => {
      (e._1.asElements above (Element.elem("IIPF") beside (Element.elem('-', e._1.asElements.width, 1) beside Element.elem("-->"))) beside e._2.asElements)
    }).reduce(_ above _)

    override def equals(obj: Any) = obj match {
      case o: InductiveIPFImpl => (o eq this) || this.alpha == o.alpha
      case _ => false
    }

    def v(that: InductiveIPFImpl): InductiveIPFImpl = that match {
      case TopIPF => TopIPF
      case _ => {
        if (that eq this) this else create(alphaUnion(this.alpha, that.alpha))
      }
    }

    def ^(that: InductiveIPFImpl): InductiveIPFImpl = that match {
      case TopIPF => this
      case _ => {
        val inter = alphaIntersection(this.alpha, that.alpha)
        if (inter.isEmpty) bottomElement else create(inter)
      }
    }

    def \(that: InductiveIPFImpl): InductiveIPFImpl = that match {
      case TopIPF => TopIPF
      case _ => {
        val diff = alphaDifference(this.alpha, that.alpha)
        if (diff.isEmpty) bottomElement else create(diff)
      }
    }

    lazy val bottomElement = create(HashMap.empty: FromType)
  }

  /**
   * This object represents the top element of the lattice of SigmaDDs.
   * It is used as a terminal.
   */
  object TopIPF extends InductiveIPFImpl(HashMap.empty) {
    
    override def listOfSubtermsVector: List[List[String]] = List(Nil)

    override def toString = "[1]"

    override lazy val size = BigInt(1)

    override def v(that: InductiveIPFImpl): InductiveIPFImpl = TopIPF

    override def ^(that: InductiveIPFImpl): InductiveIPFImpl = that

    override def \(that: InductiveIPFImpl): InductiveIPFImpl = that match {
      case TopIPF => bottomElement
      case _ => bottomElement
    }

    override def asElements: Element = Element.elem("[1]")

    override def equals(obj: Any) = obj match {
      case o: AnyRef => o eq this
      case _ => false
    }
    override lazy val hashCode = (alpha ##) + 1
  }
}