
//package scala.util.matching
package regex

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import maqicode.testing.JUnitTest
import maqicode.testing.speculum.inspect
//import scala.tools.testing.JUnitTest
//import scala.tools.testing.speculum._
import scala.reflect.ClassTag

//@RunWith(classOf[JUnit4])

class GroupNamesTest extends JUnitTest {

  //import Implicits._
  import regex.{ Mex => Gregex }

/*
  @Test def gregexExtractsNames() {
    val s = "a(?<boo>b)c"
    val (p, gs) = Gregex groups s
    val names = gs map (_.name getOrElse null)
    assertEquals("a(b)c", p)
    assertEquals(Seq("boo"), names)
  }
*/

  @Test def gregexExtractsNames(): Unit = {
    val s = "a(?<boo>b)c"
    val (p, gs) = Gregex groups s
    val names = gs map (_.name getOrElse null)
    inspect {
      "a(b)c" == p
      names == Seq("boo")
    }
  }
  @Test def regularRegexLacksGroupName(): Unit = throws[NoSuchElementException] {
    val r = """a(?<boo>b)c""".r
    val m = (r findFirstMatchIn "abc").get
    m group "boo"
  }
  @Test def gregularIgnoresEscapedGroupName(): Unit = {
    val r = """a\(\?<boo>b\)c""".r
    val t = """a(?<boo>b)c"""
    val m = (r findFirstMatchIn t).get
    inspect {
      "a(?<boo>b)c" matches """\Qa(?<boo>b)c\E"""
      "a(?<boo>b)c" matches "a\\(\\?<boo>b\\)c"
      t == m.matched
    }
    throws[NoSuchElementException] {
      null == (m group "boo")
    }
  }
  @Test def gregexIgnoresNoncapturingGroups(): Unit = {
    val p = "a(?:b)c"
    val (r, gs) = Gregex groups p
    inspect {
      p == r
      gs.isEmpty
    }
  }
  @Test def gregexFindsOptionality() {
    val s = """a(b)and(c)?and(d)*and(e)+and(f){0,100}and(g){0,}and(h){5,10}"""
    val (_, gs) = Gregex groups s
    inspect {
      !gs(0).isOption
       gs(1).isOption
       gs(2).isOption
      !gs(3).isOption
       gs(4).isOption
       gs(5).isOption
      !gs(6).isOption
    }
  }
}
