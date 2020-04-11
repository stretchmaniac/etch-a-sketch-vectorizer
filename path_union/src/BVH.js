const g = require('./geo.js');
const pathUtils = require('./path.js');

function initBVH(pathsOriginal){
    let paths = [];
    let pathIndex = 0;
    // full copy of pathsOriginal
    for(let path of pathsOriginal){
        let newPath = [];

        let max = {x: Number.NEGATIVE_INFINITY, y: Number.NEGATIVE_INFINITY};
        let min = {x: Number.POSITIVE_INFINITY, y: Number.POSITIVE_INFINITY};
        for(let v of path){
            newPath.push({x: v.x, y: v.y});
            max = g.max(max, v);
            min = g.min(min, v);
        }
        paths.push({
            bounds: {min: min, max: max},
            path: newPath,
            pathIndex: pathIndex
        });
        pathIndex++;
    }

    // the left and right indices are for construction only
    const root = {
        bounds: getBounds(paths, 0, paths.length - 1),
        leftIndex: 0,
        rightIndex: paths.length - 1,
        path: undefined, // leaf nodes only
        rightChild: undefined,
        leftChild: undefined,
    };

    partition(root, paths);
    return root;
}

function partition(node, paths){
    // leaf nodes will include one path
    if(node.rightIndex === node.leftIndex){
        node.path = paths[node.leftIndex];
        return node;
    }
    // otherwise, we need to decide which axis to split this node.
    // We'll try both axes and see which one works better 
    const median = getMedianPos(paths, node.leftIndex, node.rightIndex);
    const partitionScores = getPartitionScores(paths, node.leftIndex, node.rightIndex, median);

    const dir = partitionScores.x > partitionScores.y ? 'x' : 'y'; // some nice js hacking right here
    const leftRightIndex = sort(paths, node.leftIndex, node.rightIndex, dir, median[dir]);

    const leftChild = {
        bounds: getBounds(paths, node.leftIndex, leftRightIndex),
        leftIndex: node.leftIndex,
        rightIndex: leftRightIndex
    };

    const rightChild = {
        bounds: getBounds(paths, leftRightIndex + 1, node.rightIndex),
        leftIndex: leftRightIndex + 1,
        rightIndex: node.rightIndex
    };

    node.leftChild = leftChild;
    node.rightChild = rightChild;
    partition(leftChild, paths);
    partition(rightChild, paths);
}

// returns rightMost index of left side
function sort(paths, indexLeft, indexRight, dir, middle){
    // ah yes, we all love quicksort, don't we? 
    // (don't worry, this is just one iteration of quicksort)
    let leftFrontier = indexLeft; 
    let rightFrontier = indexRight;
    let defaultSortLeft = true;
    // indices at the frontiers have NOT been sorted yet
    while(leftFrontier <= rightFrontier){
        let el = paths[leftFrontier];
        let interval = {min: el.bounds.min[dir], max: el.bounds.max[dir]};
        let sortLeft = defaultSortLeft;
        defaultSortLeft = !defaultSortLeft; // switch default every time to even things out
        if(interval.max < middle){
            sortLeft = true;
        } else if(interval.min > middle){
            sortLeft = false;
        }

        if(sortLeft){
            leftFrontier++;
        } else {
            // swap left and right frontier elements, move right frontier to the left
            const tmp = paths[rightFrontier];
            paths[rightFrontier] = paths[leftFrontier];
            paths[leftFrontier] = tmp;
            rightFrontier--; 
        }
    }

    // I've had bad experiences in the past with BVH construction. 
    // To alleviate some of this pain, I will quell any misbehavior proactively. 
    // If you'd like to complain about how this will hurt the efficiency of the 
    // algorithm, go ahead and fix it. But on your own branch so when it breaks 
    // my version still works.
    const numElements = indexRight - indexLeft + 1;
    if(numElements <= 3){
        leftFrontier = Math.max(indexLeft, leftFrontier);
        leftFrontier = Math.min(indexRight - 1, leftFrontier);
    } else {
        leftFrontier = Math.max(indexLeft + 1, leftFrontier);
        leftFrontier = Math.min(indexRight - 2, leftFrontier);
    }
    return leftFrontier;
}

function getMedianPos(paths, indexLeft, indexRight){
    // turns out calculating the median of an unsorted list is not so straightforward
    // guess we'll just go with the mean 
    let total = {x: 0, y: 0};
    for(let i = indexLeft; i <= indexRight; i++){
        const b = paths[i].bounds;
        const boxMX = (b.min.x + b.max.x) / 2;
        const boxMY = (b.min.y + b.max.y) / 2;
        total.x += boxMX;
        total.y += boxMY;
    }
    const n = indexRight - indexLeft + 1;
    total.x /= n;
    total.y /= n;
    return total;
}

function getPartitionScores(paths, indexLeft, indexRight, position){
    let leftSideCounts = {x: 0, y: 0};
    let rightSideCounts = {x: 0, y: 0};
    
    for(let i = indexLeft; i <= indexRight; i++){
        const b = paths[i].bounds;
        if(b.max.x < position.x){
            leftSideCounts.x++;
        }
        if(b.max.y < position.y){
            leftSideCounts.y++;
        }
        if(b.min.x > position.x){
            rightSideCounts.x++;
        }
        if(b.min.y > position.y){
            rightSideCounts.y++;
        }
    }

    const fx = leftSideCounts.x == 0 || rightSideCounts.x == 0 ? 0 : 
        Math.min(leftSideCounts.x / rightSideCounts.x, rightSideCounts.x / leftSideCounts.x);
    const fy = leftSideCounts.y == 0 || rightSideCounts.y == 0 ? 0 : 
        Math.min(leftSideCounts.y / rightSideCounts.y, rightSideCounts.y / leftSideCounts.y);
    return {
        x: fx * (leftSideCounts.x + rightSideCounts.x),
        y: fy * (leftSideCounts.y + rightSideCounts.y)
    };
}

// indexLeft, indexRight both inclusive
function getBounds(paths, indexLeft, indexRight){
    let max = {x: Number.NEGATIVE_INFINITY, y: Number.NEGATIVE_INFINITY};
    let min = {x: Number.POSITIVE_INFINITY, y: Number.POSITIVE_INFINITY};

    for(let i = indexLeft; i <= indexRight; i++){
        max = g.max(max, paths[i].bounds.max);
        min = g.min(min, paths[i].bounds.min);
    }

    return {min: min, max: max};
}

function minBoundsDist(bounds1, bounds2){
    let dx = 0;
    let dy = 0;
    
    dx = Math.max(dx, bounds1.min.x - bounds2.max.x);
    dx = Math.max(dx, bounds2.min.x - bounds1.max.x);
    dy = Math.max(dy, bounds1.min.y - bounds2.max.y);
    dy = Math.max(dy, bounds2.min.y - bounds1.max.y);

    return Math.sqrt(dx * dx + dy * dy);
}

function nearestNode(root, indexList){
    // bvhs are wonderful in a lot of ways. One of those ways is how (seemingly)
    // very annoying looking algorithms get turned into elegant recursion functions

    // first traverse the tree, annotating nodes as we go
    function f(node, d){
        if(node.path){
            node.containsTarget = indexList.includes(node.path.pathIndex);
            node.hasFreeNode = !node.containsTarget;
        } else {
            f(node.leftChild, d+1);
            f(node.rightChild, d+1);
            node.containsTarget = node.leftChild.containsTarget || node.rightChild.containsTarget;
            node.hasFreeNode = node.leftChild.hasFreeNode || node.rightChild.hasFreeNode;
        }
    }
    f(root, 1);

    let bestDist = Number.POSITIVE_INFINITY;
    let bestConnection = undefined;
    let bestIndex = 0;
    let bestRootIndex = 0;
    // node contains a free node, againstNode contains a target node
    function search(node, againstNode){
        if(node.path && againstNode.path){
            const segment = pathUtils.shortestConnectingLineSegment(againstNode.path.path, node.path.path);
            const len = g.distance(segment[0], segment[1]);
            if(len < bestDist){
                bestDist = len;
                bestConnection = segment;
                bestIndex = node.path.pathIndex;
                bestRootIndex = againstNode.path.pathIndex;
            }
            return;
        }

        if(node.path){
            for(let againstChild of [againstNode.leftChild, againstNode.rightChild]){
                if(againstChild.containsTarget && minBoundsDist(node.bounds, againstChild.bounds) < bestDist){
                    search(node, againstChild);
                }
            }
            return;
        } 

        for(let nodeChild of [node.leftChild, node.rightChild]){
            if(nodeChild.hasFreeNode && minBoundsDist(nodeChild.bounds, againstNode.bounds) < bestDist){
                search(nodeChild, againstNode);
            }
        }
    }
    search(root, root);

    return {
        pathIndex: bestIndex,
        transition: bestConnection,
        rootIndex: bestRootIndex
    };
}

module.exports = {
    initBVH: initBVH,
    nearestNode: nearestNode
};