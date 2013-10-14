package org.basex.query.ft;

import static org.basex.query.QueryText.*;

import org.basex.data.*;
import org.basex.query.*;
import org.basex.query.expr.*;
import org.basex.query.util.*;
import org.basex.query.value.node.*;
import org.basex.query.var.*;
import org.basex.util.*;
import org.basex.util.ft.*;
import org.basex.util.hash.*;

/**
 * FTDistance expression.
 *
 * @author BaseX Team 2005-13, BSD License
 * @author Christian Gruen
 */
public final class FTDistance extends FTFilter {
  /** Distance. */
  private final Expr[] dist;

  /**
   * Constructor.
   * @param ii input info
   * @param e expression
   * @param d distances
   * @param u unit
   */
  public FTDistance(final InputInfo ii, final FTExpr e, final Expr[] d, final FTUnit u) {
    super(ii, e, u);
    dist = d;
  }

  @Override
  public void checkUp() throws QueryException {
    checkNoneUp(dist);
    super.checkUp();
  }

  @Override
  public FTExpr compile(final QueryContext ctx, final VarScope scp) throws QueryException {
    for(int d = 0; d < dist.length; d++) dist[d] = dist[d].compile(ctx, scp);
    return super.compile(ctx, scp);
  }

  @Override
  protected boolean filter(final QueryContext ctx, final FTMatch mtc, final FTLexer lex)
      throws QueryException {

    final long min = checkItr(dist[0], ctx);
    final long max = checkItr(dist[1], ctx);
    mtc.sort();

    final FTMatch match = new FTMatch();
    FTStringMatch last = null, first = null;
    for(final FTStringMatch sm : mtc) {
      if(sm.exclude) {
        match.add(sm);
      } else {
        if(first == null) {
          first = sm;
        } else {
          final int d = pos(sm.start, lex) - pos(last.end, lex) - 1;
          if(d < min || d > max) return false;
        }
        last = sm;
      }
    }
    first.end = last.end;
    mtc.reset();
    mtc.add(first);
    mtc.add(match);
    return true;
  }

  @Override
  public boolean has(final Flag flag) {
    for(final Expr d : dist) if(d.has(flag)) return true;
    return super.has(flag);
  }

  @Override
  public boolean removable(final Var v) {
    for(final Expr d : dist) if(!d.removable(v)) return false;
    return super.removable(v);
  }

  @Override
  public VarUsage count(final Var v) {
    return super.count(v).plus(VarUsage.sum(v, dist));
  }

  @Override
  public FTExpr inline(final QueryContext ctx, final VarScope scp, final Var v, final Expr e)
      throws QueryException {
    return inlineAll(ctx, scp, expr, v, e) | inlineAll(ctx, scp, dist, v, e)
        ? optimize(ctx, scp) : null;
  }

  @Override
  public FTExpr copy(final QueryContext ctx, final VarScope scp, final IntObjMap<Var> vs) {
    return new FTDistance(info, expr[0].copy(ctx, scp, vs),
        Arr.copyAll(ctx, scp, vs, dist), unit);
  }

  @Override
  public void plan(final FElem plan) {
    addPlan(plan, planElem(DISTANCE, dist[0] + "-" + dist[1] + ' ' + unit), expr);
  }

  @Override
  public String toString() {
    return super.toString() + DISTANCE + PAR1 + dist[0] + '-' + dist[1] + ' ' + unit + PAR2;
  }

  @Override
  public boolean accept(final ASTVisitor visitor) {
    return super.accept(visitor) && visitAll(visitor, dist);
  }

  @Override
  public int exprSize() {
    int sz = 1;
    for(final FTExpr e : expr) sz += e.exprSize();
    for(final Expr e : dist) sz += e.exprSize();
    return sz;
  }
}