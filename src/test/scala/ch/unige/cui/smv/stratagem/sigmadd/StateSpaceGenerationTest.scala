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

import org.scalatest.FlatSpec

import ch.unige.cui.smv.stratagem.adt.ADT
import ch.unige.cui.smv.stratagem.adt.ATerm
import ch.unige.cui.smv.stratagem.adt.Signature
import ch.unige.cui.smv.stratagem.ts.Choice
import ch.unige.cui.smv.stratagem.ts.DeclaredStrategyInstance
import ch.unige.cui.smv.stratagem.ts.Identity
import ch.unige.cui.smv.stratagem.ts.One
import ch.unige.cui.smv.stratagem.ts.Sequence
import ch.unige.cui.smv.stratagem.ts.Strategy
import ch.unige.cui.smv.stratagem.ts.TransitionSystem
import ch.unige.cui.smv.stratagem.ts.Union
import ch.unige.cui.smv.stratagem.ts.VariableStrategy
// scalastyle:off regex
/**
 * Tests the generation of the state space.
 * @author mundacho
 *
 */
class StateSpaceGenerationTest extends FlatSpec {

  val signature = (new Signature)
    .withSort("ph")
    .withSort("state")
    .withSort("fork")
    .withSort("cluster")
    .withGenerator("eating", "state")
    .withGenerator("thinking", "state")
    .withGenerator("waiting", "state")
    .withGenerator("waitingForLeftFork", "state")
    .withGenerator("waitingForRightFork", "state")
    .withGenerator("forkUsed", "fork")
    .withGenerator("forkFree", "fork")
    .withGenerator("emptytable", "ph")
    .withGenerator("philo", "ph", "state", "fork", "ph")
    .withGenerator("c", "cluster", "ph", "cluster")

  val adt = new ADT("philoModel", signature)
    .declareVariable("x", "fork")
    .declareVariable("p", "ph")
    .declareVariable("s", "state")
    .declareVariable("f", "fork")
  // definitions to simplify the reading of terms.
  def eating = adt.term("eating")
  def thinking = adt.term("thinking")
  def waiting = adt.term("waiting")
  def waitingForLeftFork = adt.term("waitingForLeftFork")
  def waitingForRightFork = adt.term("waitingForRightFork")
  def forkUsed = adt.term("forkUsed")
  def forkFree = adt.term("forkFree")
  def emptytable = adt.term("emptytable")
  def X = adt.term("x")
  def P = adt.term("p")
  def S = adt.term("s")
  def F = adt.term("f")
  def philo(state: ATerm, fork: ATerm, ph: ATerm) = adt.term("philo", state, fork, ph)

  // definitions to simplify strategy declarations
  def Repeat(s: Strategy) = DeclaredStrategyInstance("repeat", s)
  def DoForAllPhils(s: Strategy) = DeclaredStrategyInstance("doForAllPhil", s)
  def DoForLastPhil(s: Strategy) = DeclaredStrategyInstance("doForLastPhil", s)

  val V = VariableStrategy("V")

  def generateInitialState(n: Int): ATerm = {
    require(n > 1)
    def auxGenerate(n: Int): ATerm = {
      n match {
        case 1 => philo(thinking, forkFree, emptytable)
        case _ => philo(thinking, forkFree, auxGenerate(n - 1))
      }
    }
    auxGenerate(n)
  }

  val numberOfPhilosophers = 3

  val ts = (new TransitionSystem(adt, generateInitialState(numberOfPhilosophers)))
    .declareStrategy("doForAllPhil", V) { Union(V, Choice(One(DoForAllPhils(V)), Identity)) }(false)
    .declareStrategy("doForLastPhil", V) { Choice(One(DoForLastPhil(V)), V) }(false)
    .declareStrategy("goToWaitPhilo", philo(thinking, X, P) -> philo(waiting, X, P))(false)
    .declareStrategy("goToWait") { DoForAllPhils(DeclaredStrategyInstance("goToWaitPhilo")) }(true)
    .declareStrategy("takeRightForkFromWaitingPhilo", philo(waiting, forkFree, P) -> philo(waitingForLeftFork, forkUsed, P))(false)
    .declareStrategy("takeRightForkFromWaiting") { DoForAllPhils(DeclaredStrategyInstance("takeRightForkFromWaitingPhilo")) }(true)
    .declareStrategy("takeRightForkFromWaitingForRightForkPhilo", philo(waitingForRightFork, forkFree, P) -> philo(eating, forkUsed, P))(false)
    .declareStrategy("takeRightForkFromWaitingForRightFork") { DoForAllPhils(DeclaredStrategyInstance("takeRightForkFromWaitingForRightForkPhilo")) }(true)
    .declareStrategy("takeLeftForkFromWaitingPhilo", philo(S, forkFree, philo(waiting, F, P)) -> philo(S, forkUsed, philo(waitingForRightFork, F, P)))(false) // this rule is not applied!
    .declareStrategy("takeLeftForkFromWaiting") { DoForAllPhils(DeclaredStrategyInstance("takeLeftForkFromWaitingPhilo")) }(true)
    .declareStrategy("takeLeftForkFromWaitingForLeftForkPhilo", philo(S, forkFree, philo(waitingForLeftFork, forkUsed, P)) -> philo(S, forkUsed, philo(eating, forkUsed, P)))(false)
    .declareStrategy("takeLeftForkFromWaitingForLeftFork") { DoForAllPhils(DeclaredStrategyInstance("takeLeftForkFromWaitingForLeftForkPhilo")) }(true)
    .declareStrategy("goToThinkPhilo", philo(S, forkUsed, philo(eating, forkUsed, P)) -> philo(S, forkFree, philo(thinking, forkFree, P)))(false)
    .declareStrategy("goToThink") { DoForAllPhils(DeclaredStrategyInstance("goToThinkPhilo")) }(true)
    .declareStrategy("takeLeftForkWaitingPhilo1", philo(waiting, F, P) -> philo(waitingForRightFork, F, P))(false)
    .declareStrategy("takeRightFork", philo(S, forkFree, P) -> philo(S, forkUsed, P))(false)
    .declareStrategy("takeLeftForkFromWaitingPhilo1") { Sequence(DeclaredStrategyInstance("takeLeftForkWaitingPhilo1"), DoForLastPhil(DeclaredStrategyInstance("takeRightFork"))) }(true)
  // there are some rules missing, but the state space is the same size.
  // We intentionally omit the rules to make the first philosopher go to eat after taking the right fork and also the rule to make him go back to eat.

  def time[R](block: => R): R = {
    val t0 = System.nanoTime()
    val result = block // call-by-name
    val t1 = System.nanoTime()
    println("Elapsed time: " + (t1 - t0) * 1.0e-9 + "[seconds]")
    result
  }

  def stats[R](block: => R): R = {
    val result = block // call-by-name

    println("Total cache hits: " + SigmaDDRewritingCacheStats.hitCounter)
    println("Total rewrites: " + SigmaDDRewritingCacheStats.callsCounter)
    println("Cache hits to rewrites ratio: " + 100 * SigmaDDRewritingCacheStats.hitCounter / SigmaDDRewritingCacheStats.callsCounter + "%")
    result
  }

  "DeclaredStrategies" should "allow to generate the state space for the philosophers problem with 3 philosophers" in {
    val rewriter = SigmaDDRewriterFactory.transitionSystemToStateSpaceRewriter(ts)
    stats(time(println("Total number of states: " + rewriter(SigmaDDFactoryImpl.create(ts.initialState)).get.size)))
    assert(rewriter(SigmaDDFactoryImpl.create(ts.initialState)).get.size == 76)
  }

  val ts1 = (new TransitionSystem(adt, generateInitialState(numberOfPhilosophers)))
    .declareStrategy("doForAllPhil", V) { Union(V, Choice(One(DoForAllPhils(V)), Identity)) }(false)
    .declareStrategy("doForLastPhil", V) { Choice(One(DoForLastPhil(V)), V) }(false)
    .declareStrategy("goToWaitPhilo", philo(thinking, X, P) -> philo(waiting, X, P))(false)
    .declareStrategy("goToWait") { DoForAllPhils(DeclaredStrategyInstance("goToWaitPhilo")) }(false)
    .declareStrategy("takeRightForkFromWaitingPhilo", philo(waiting, forkFree, P) -> philo(waitingForLeftFork, forkUsed, P))(false)
    .declareStrategy("takeRightForkFromWaiting") { DoForAllPhils(DeclaredStrategyInstance("takeRightForkFromWaitingPhilo")) }(false)
    .declareStrategy("takeRightForkFromWaitingForRightForkPhilo", philo(waitingForRightFork, forkFree, P) -> philo(eating, forkUsed, P))(false)
    .declareStrategy("takeRightForkFromWaitingForRightFork") { DoForAllPhils(DeclaredStrategyInstance("takeRightForkFromWaitingForRightForkPhilo")) }(false)
    .declareStrategy("takeLeftForkFromWaitingPhilo", philo(S, forkFree, philo(waiting, F, P)) -> philo(S, forkUsed, philo(waitingForRightFork, F, P)))(false) // this rule is not applied!
    .declareStrategy("takeLeftForkFromWaiting") { DoForAllPhils(DeclaredStrategyInstance("takeLeftForkFromWaitingPhilo")) }(true)
    .declareStrategy("takeLeftForkFromWaitingForLeftForkPhilo", philo(S, forkFree, philo(waitingForLeftFork, forkUsed, P)) -> philo(S, forkUsed, philo(eating, forkUsed, P)))(false)
    .declareStrategy("takeLeftForkFromWaitingForLeftFork") { DoForAllPhils(DeclaredStrategyInstance("takeLeftForkFromWaitingForLeftForkPhilo")) }(false)
    .declareStrategy("goToThinkPhilo", philo(S, forkUsed, philo(eating, forkUsed, P)) -> philo(S, forkFree, philo(thinking, forkFree, P)))(false)
    .declareStrategy("goToThink") { DoForAllPhils(DeclaredStrategyInstance("goToThinkPhilo")) }(false)
    .declareStrategy("takeLeftForkWaitingPhilo1", philo(waiting, F, P) -> philo(waitingForRightFork, F, P))(false)
    .declareStrategy("takeRightFork", philo(S, forkFree, P) -> philo(S, forkUsed, P))(false)
    .declareStrategy("takeLeftForkFromWaitingPhilo1") { Sequence(DeclaredStrategyInstance("takeLeftForkWaitingPhilo1"), DoForLastPhil(DeclaredStrategyInstance("takeRightFork"))) }(true)
  // there are some rules missing, but the state space is the same size.
  // We intentionally omit the rules to make the first philosopher go to eat after taking the right fork and also the rule to make him go back to eat.

}