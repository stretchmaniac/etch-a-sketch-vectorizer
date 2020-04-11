const fs = require('fs');
const bvh = require('./BVH.js');
const g = require('./geo.js');

function init(){
    // first command line option
    const jsonFilePath = process.argv[2];
    const jsonOutputPath = process.argv[3];

    fs.readFile(jsonFilePath, 'utf8', (err, jsonString) => {
        if (err) {
            console.log("File read failed:", err)
            return
        }
        writeResult(pathUnion(JSON.parse(jsonString)), jsonOutputPath);
    })
}

function pathUnion(pathsJson){
    // input should be of the form 
    /*
        { 
            "paths": [
                [{x:x0, y:y0}, {x:x1, y:y1}, {x:x2, y:y2}, ..., {x:xn, y:yn}], // first polygonal line path
                [{x:x0, y:y0}, {x:x1, y:y1}, {x:x2, y:y2}, ..., {x:xm, y:ym}], // second polygonal line path
                ...
            ]
        }
    */

    // say, who remembers CSCI 4041?
    // That's right, it's time for a minimum spanning tree!
    // Each path will be a node, with edges between any two nodes weighted with the minimum distance between 
    // the two paths. Two paths that intersect, for example, are connected by an edge with weight zero.

    // In order for efficient computation of minimum distance between paths, we use BVHs (Bounding Volume Hierarchies).
    // We'll use Prim's algorithm (slightly modified) for the minimum spanning tree construction

    // first dissect paths into a set of paths where each path is one line segment 
    let BVHPaths = [];
    const paths = pathsJson.paths;
    for(let p of paths){
        let workingBase = p[0];
        for(let i = 1; i < p.length; i++){
            BVHPaths.push([workingBase, p[i]]);
            workingBase = p[i];
        }
    }

    // construct a BVH from these path parts
    let bvhRoot = bvh.initBVH(BVHPaths);

    // now get to work
    if(BVHPaths.length === 0) {
        return [];
    }
    
    const spanningTreeRoot = {
        children:[],
        pathIndex:0,
        transition: [BVHPaths[0][0], BVHPaths[0][0]] // degenerate transition into this path
    }
    const indexToTreeNode = {};
    indexToTreeNode[0] = spanningTreeRoot;

    const targetNodes = [0];

    while(targetNodes.length < BVHPaths.length){
        const nextNode = bvh.nearestNode(bvhRoot, targetNodes);
        targetNodes.push(nextNode.pathIndex);
        const newNode = {
            children:[],
            pathIndex: nextNode.pathIndex,
            transition: nextNode.transition
        };
        indexToTreeNode[nextNode.rootIndex].children.push(newNode);
        indexToTreeNode[newNode.pathIndex] = newNode;
    }

    // now we need to make a path out of the spanning tree
    let finalPaths = [];
    let moveEpsilon = 0.0000001;

    let headPos = BVHPaths[0][0];

    function moveTo(position){
        if(g.distance(headPos, position) > moveEpsilon){
            finalPaths.push(position);
        }
        headPos = position;
    }

    function traverse(node){
        // first do incoming transition, then traverse all the children, then draw 
        // the path itself, then do outgoing transition 
        moveTo(node.transition[0]);
        moveTo(node.transition[1]);
        for(let child of node.children){
            traverse(child);
        }
        const path = BVHPaths[node.pathIndex];
        if(g.distance(headPos, path[0]) < g.distance(headPos, path[1])){
            moveTo(path[0]);
            moveTo(path[1]);
        } else {
            moveTo(path[1]);
            moveTo(path[0]);
        }
        moveTo(node.transition[1]);
        moveTo(node.transition[0]);
    }    

    traverse(spanningTreeRoot);

    return finalPaths;
}

function writeResult(result, filePath){
    const jsonOutput = JSON.stringify({path: result});
    fs.writeFile(filePath, jsonOutput, function(err) {
        console.log(err);
    });
}

init();