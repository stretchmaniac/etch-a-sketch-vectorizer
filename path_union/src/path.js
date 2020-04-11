const g = require('./geo.js');
// returns segments that goes from lineSegment1 to lineSegment2
function shortestConnectingLineSegment(lineSegment1, lineSegment2){
    // each line segment is a list [{x0, y0}, {x1, y1}]
    const a = lineSegment1[0];
    const b = lineSegment1[1];
    const v1 = g.sub(b, a);
    const v1Norm = g.normalize(v1);

    const c = lineSegment2[0];
    const d = lineSegment2[1];
    const v2 = g.sub(d, c);
    const v2Norm = g.normalize(v2);

    const ca = g.sub(a, c);
    const cb = g.sub(b, c);
    const aDist = g.cross2d(v2Norm, ca);
    const bDist = g.cross2d(v2Norm, cb);
    if(aDist != bDist){
        const t = aDist / (aDist - bDist);
        
        const ac = g.sub(c, a);
        const ad = g.sub(d, a);
        const cDist = g.cross2d(v1Norm, ac);
        const dDist = g.cross2d(v1Norm, ad);
        if(cDist != dDist){
            const u = cDist / (cDist - dDist);

            if(t >= 0 && t <= 1 && u >= 0 && u <= 1){
                // line segments intersect 
                const intPt = g.add(a, g.mult(v1, t));
                return [intPt, intPt];
            }
        }
    }
    // if no intersection, then the shortest connecting line segment is incident to 
    // (at least) one vertex of one of the line segments.
    let bestDist = Number.POSITIVE_INFINITY;
    let bestPair = [];
    
    let f = (x, y, yz, z) => {
        // test pt x against line segment [y, z]
        const projectVal = g.dot(g.sub(x, y), yz) / g.length(yz)**2;
        let nearest = undefined;
        if(projectVal >= 0 && projectVal <= 1){
            return g.add(y, g.mult(yz, projectVal));
        }
        if(g.distance(x, y) < g.distance(x, z)){
            return y;
        } 
        return z;
    }

    for(let p of lineSegment1){
        const near = f(p, c, v2, d);
        const len = g.distance(p, near);
        if(len < bestDist){
            bestPair = [p, near];
            bestDist = len;
        }
    }
    for(let p of lineSegment2){
        const near = f(p, a, v1, b);
        const len = g.distance(p, near);
        if(len < bestDist){
            bestPair = [near, p];
            bestDist = len;
        }
    }
    return bestPair;
}

module.exports = {
    shortestConnectingLineSegment: shortestConnectingLineSegment
};