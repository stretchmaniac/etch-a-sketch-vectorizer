// It seems every time I start something new I have to make a vector library.

function add(v1, v2){
    return {x: v1.x + v2.x, y: v1.y + v2.y};
}

function sub(v1, v2){
    return {x: v1.x - v2.x, y: v1.y - v2.y};
}

function mult(v, scalar){
    return {x: v.x * scalar, y: v.y * scalar};
}

function dot(v1, v2){
    return v1.x * v2.x + v1.y * v2.y;
}

function min(v1, v2){
    return {x: Math.min(v1.x, v2.x), y: Math.min(v1.y, v2.y)};
}

function max(v1, v2){
    return {x: Math.max(v1.x, v2.x), y: Math.max(v1.y, v2.y)};
}

function length(a){
    return Math.sqrt(a.x*a.x + a.y*a.y);
}

function normalize(a){
    let m = length(a);
    if(m == 0){
        return a;
    }
    return mult(a, 1/m);
}

function cross2d(a, b){
    return a.x*b.y - b.x*a.y;
}

// project a onto b
function project(a, b){
    return mult(b, dot(a,b)/dot(b,b));
}

function distance(a,b){
    return length(sub(a,b));
}

module.exports = {
    add: add,
    sub: sub,
    mult: mult,
    dot: dot,
    min: min,
    max: max,
    length: length,
    normalize: normalize,
    cross2d: cross2d,
    project: project,
    distance: distance
};