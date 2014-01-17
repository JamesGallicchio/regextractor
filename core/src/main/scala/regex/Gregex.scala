
package object regex {
  import scala.language.experimental.macros
  implicit class Gr(val s: String) extends AnyVal {
    /** A regex with some group smarts.
     *  In particular, it will pick up inline group names
     *  and optionality, in order to unapply to
     *  tuples with type Option[String] in positions
     *  where normally null could be returned.
     */
    def gr: Any = macro Mex.gr
  }
  /** Supplies a group-aware regex interpolator. */
  implicit class Grinch(val sc: StringContext) /*extends AnyVal*/ {
    object gr {
      def apply(args: Any*): Any  = macro Mex.grin
      def unapply(s: String): Any = macro Mex.grout
    }
  }
}

package regex {
  import scala.language.existentials
  import scala.language.experimental.macros
  import scala.util.matching.Regex

  /** Group-aware regex.
   */
  class Gregex[A](regex: String, groups: String*) extends Regex(regex, groups: _*) {

    /** Regextractor!
     *  The result is essentially `Option[A]`, though for the multi-value
     *  case, extra boxing is avoided by the macro.
     */
    def unapply(s: String): Any = macro Mex.regextractor[A]
  }
}
