
package regex {
  import scala.language.experimental.macros
  //import scala.reflect.macros.{ WhiteboxContext => Context }
  import scala.reflect.macros.whitebox.Context
  import scala.util.matching.Regex
  import scala.collection.mutable.{ ListBuffer, Stack }
  import java.util.regex.{ Pattern, PatternSyntaxException }

  /** Support for group-aware regex.
   */
  private[regex] object Mex {

    type BadPat = IllegalStateException
    private[this] def *?! = throw new IllegalStateException

    /** A regex group optionally has a name and optionally
     *  is optional, that is, might be null when a match
     *  succeeds.  Groups that are optional will be extracted
     *  as an `Option[String]` by the regextractor macro.
     */
    case class Groupex(name: Option[String], isOption: Boolean, isCapture: Boolean)

    /** The regex extractor delegates to the `Regex`
     *  and wraps group values in `Option` as required
     *  by the desired result `A`.
     */
    def regextractor[A: c.WeakTypeTag](c: Context { type PrefixType = { def unapplySeq(s: String): Option[Seq[String]]} })(s: c.Expr[String]) = {
      import c.universe._
      val at = implicitly[c.WeakTypeTag[A]]
      val xs = at.tpe match {
        //case TypeRef(_, NothingClass, args) =>
        case zip if at.tpe =:= typeOf[Nothing] => List.empty[Tree]
        //case one if at.tpe =:= typeOf[String]  =>
        //case opt if at.tpe =:= typeOf[Option[String]] =>
        case TypeRef(_, _, args) => args.zipWithIndex map {
          case (TypeRef(_, _, Nil), i) => // String
            val m = TermName(s"_${i+1}")
            q"def $m = captured.get($i)"
          case (_, i)                  => // Option[String]
            val m = TermName(s"_${i+1}")
            q"def $m = Option(captured.get($i))"
        }
      }
      q"""
        new {
          var captured: Option[Seq[String]] = None
          def isEmpty = captured.isEmpty
          def get = this
          ..$xs
          def unapply(x: String) = {
            captured = ${c.prefix.tree}.unapplySeq(x)
            this
          }
        }.unapply($s)
      """
    }

    /** For a constant string, gr"s" is the same as "s".gr.
     *  Otherwise, gr"$s" is the same as s"$s".r with group names,
     *  that is, without customized unapply.
     */
    def grin(c: Context)(args: c.Expr[Any]*): c.Expr[Any] = {
      import c.universe._
      val res = c.prefix.tree match {
        case q"$_($_(${Literal(Constant(s: String))})).$_" => // ext(sc(s))
          grFrom(c, s)
        case q"$_($sc).$_" => // ext(sc(s))
          val x = TermName(c.freshName)
          q"""
            val $x = Mex groups $sc.s(..$args)
            new scala.util.matching.Regex($x._1, ($x._2 map (_.name getOrElse null)): _*)
          """
        case other =>
          c.error(c.prefix.tree.pos, s"Unexpected prefix ${showRaw(other)}")
          *?!
      }
      c.Expr(res)
    }

    /** On unapply, the number of $holes must equal the number
     *  of capturing groups. As syntax enforcement, a warning
     *  is emitted if holes do not precede their corresponding
     *  group.  This makes `f"$i%d"` analogous to `gr"$i(\d+)"`
     *  or similarly `gr"${IntOf(i)}(\d+)"`.
     *
     *  A missing group is supplied as `"(.*)"`, by analogy to
     *  the "%s" supplied in `f"$s"`.
     *
     *  TODO make `gr"a(?<boo>b)c"` like `gr"a$boo(b)c"`
     *  then groups and holes would not balance in number.
     */
    def grout(c: Context)(s: c.Expr[String]): c.Expr[Any] = {
      import c.universe._
      def fail(msg: String) = c.abort(c.enclosingPosition, msg)
      val plug = "(.*)"  // group added to plug a hole
      val t = c.macroApplication match {
        case q"$_($_(..$parts)).gr.unapply($unargs)" =>
          val ps = parts map {
            case Literal(Constant(s: String)) => s
            case t => fail(s"Inconstant part: ${showRaw(t)}") ; ""
          }
          val qs = ps.head +: (ps.tail map { s =>
            if (s startsWith s"$LP") s
            else plug + s
          })
          val (rx, gxs) = Mex groups qs.mkString
          val p = Literal(Constant(rx))
          if (gxs.size != qs.size - 1) fail(s"${gxs.size} groups to extract ${qs.size - 1} strings.")
          //val gs = 1 to gxs.size map (_ => Literal(Constant("")))  // ignore group names
          def m(i: Int) = TermName(s"_${i+1}")
          def xs =
            gxs.zipWithIndex map {
              case (g, i) if g.isOption =>
                q"def ${m(i)} = Option(captured.get($i))"
              case (g, i) =>
                q"def ${m(i)} = captured.get($i)"
            }
          val res =
            if (gxs.isEmpty)
              q"""
                new {
                  val r = new scala.util.matching.Regex($p)
                  def unapply(x: String): Boolean = r.unapplySeq(x).nonEmpty
                }.unapply($unargs)
              """
            else
              q"""
                new {
                  var captured: Option[Seq[String]] = None
                  def isEmpty = captured.isEmpty
                  def get = this
                  ..$xs
                  def unapply(x: String) = {
                    val r = new scala.util.matching.Regex($p)
                    captured = r.unapplySeq(x)
                    this
                  }
                }.unapply($unargs)
              """
//Console println s"grout is $res"
          res
        case _ => c.abort(c.enclosingPosition, "Unexpected application") ; EmptyTree
      }
      c.Expr[Any](t)
    }

    /** Conjure a Gregex of the appropriate type.
     *  The type parameter captures what the unapply returns,
     *  viz., `Option[String]` for groups which can match
     *  null input.
     */
    def gr(c: Context) = {
      import c.universe._

      // if the pattern is a literal, we can figure out the groups
      c.prefix.tree match {
        case q"$_(${ Literal(Constant(s: String)) })" => grFrom(c, s)
        case _ => *?!
      }
    }

    def grFrom(c: Context, s: String) = {
      import c.universe._

      val (p, names) = try {
        Mex groups s
      } catch {
        case _: BadPat => (s, Nil)    // if really bad, fail compile below
      }

      // type params for Gregex can be Nothing if no groups,
      // Option[String] or String if one group, else a tuple (String, Option[String], ...)
      val tps = names map { n =>
        if (n.isOption) tq"Option[String]" else tq"String"
      }
      val tp =
        if (tps.isEmpty) {
          tq"Nothing"
        } else if (tps.length == 1) {
          tps.head  // change to Tuple1[String]
        } else {
          tq"(..$tps)"
        }
      val ns = names map (_.name getOrElse null)
      try Pattern compile p
      catch {
        case e: PatternSyntaxException => c.error(c.macroApplication.pos, e.getMessage)
      }
      q"new Gregex[$tp]($p, ..$ns)"
    }

    private final val BS = '\\'
    private final val LP = '('
    private final val RP = ')'
    private final val ?  = '?'
    private final val QE = 'Q'
    private final val EQ = 'E'
    private final val OP = '<'
    private final val CL = '>'
    private final val CO = ':'

    /** Extract inline group names from regex.
     *  Returns a pattern string with the inline group
     *  names stripped and \Q\E sequences converted to
     *  ordinary escapes (TODO). The groups are returned
     *  as a sequence of `Groupex` to indicate the
     *  group name, if any, and whether the group
     *  can match empty input, i.e., whether group
     *  value could be `null` and will therefore be
     *  extracted as `Option[String]`.
     */
    def groups(s: String): (String, Seq[Groupex]) = {
      val p = new StringBuilder(s.length)
      val ns = ListBuffer.empty[Groupex]
      // looking for (?<name>
      //val r = """(?<!\\)\(\?<(\p{Alpha}\p{Alnum}*)>""".r
      //for (m <- r findAllMatchIn s) ns += m group 1
      var inQ = false
      var esc = false
      val stk = Stack.empty[Groupex]
      var i = 0
      def cur = s(i)
      def next = {
        skip()
        cur
      }
      def skip() = i += 1
      def unskip() = i -= 1
      def put(c: Char) = p append c
      def putc = put(cur)
      def more = i < s.length         // cur is OK
      def hasNext = i < s.length - 1
      def name = {
        skip()
        if (cur.isLetter) {
          val nb = new StringBuilder
          do {
            nb append cur
            skip()
          } while (more && cur.isLetterOrDigit)
          if (cur != CL) *?!
          nb.toString
        } else *?!
      }
      def anonGroup       = Groupex(None, isOption = false, isCapture = true)
      def nonCaptureGroup = Groupex(None, isOption = false, isCapture = false)
      // start of group, named or anon
      def group = next match {
        case `?` => next match {
          case OP => Groupex(Some(name), isOption = false, isCapture = true)
          case CO => unskip() ; unskip() ; nonCaptureGroup
          case _  => unskip() ; unskip() ; anonGroup
        }
        case _   => unskip() ; anonGroup
      }
      def number = if (!cur.isDigit) Int.MaxValue else {
        val sb = new StringBuilder
        while (cur.isDigit) {
          sb append cur
          putc
          skip()
        }
        sb.toInt
      }
      def range = {
        skip()
        if (!cur.isDigit) *?!
        val i = number
        cur match {
          case c @ ',' => put(c) ; skip() ; (i, number)
          case '}' => (i, i)
        }
      }
      val NoRange = (-1, -1)
      // *, ?, +, {0, 2}
      def quantifier: (Int, Int) = if (!hasNext) NoRange else {
        next match {
          case c @ '*' => put(c) ; (0, Int.MaxValue)
          case c @ '?' => put(c) ; (0, 1)
          case c @ '+' => put(c) ; (1, Int.MaxValue)
          case c @ '{' =>
            put(c)
            val r = range
            cur match {
              case c @ '}' => put(c) ; r
              case _   => *?!
            }
          case c       => unskip() ; NoRange
        }
      }
      while (i < s.length) {
        if (inQ) {
          cur match {
            case EQ => inQ = false
            case c  => put(c)
          }
        } else if (esc) {
          cur match {
            case QE => inQ = true
            case c  => p append BS append cur
          }
          esc = false
        } else {
          cur match {
            case BS => esc = true
            case LP => put(LP) ; stk push group
            case RP =>
              put(RP)
              if (stk.isEmpty) *?!
              val g = stk.pop
              if (g.isCapture) ns += (g copy (isOption = quantifier._1 == 0))
            case c  => put(c)
          }
        }
        skip()
      }
      if (inQ || esc || stk.nonEmpty) *?!
      (p.toString, ns.toSeq)
    }
  }
}
