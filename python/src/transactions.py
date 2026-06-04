from dataclasses import replace


def transact(db, ops):
    working = db
    for op in ops:
        working = op(working)
    final_layer = working.layers[-1]
    return replace(db, layers=db.layers + [final_layer], top_id=working.top_id, curr_time=db.curr_time + 1)


def what_if(db, ops):
    working = db
    for op in ops:
        working = op(working)
    final_layer = working.layers[-1]
    return replace(db, layers=db.layers + [final_layer], top_id=working.top_id)
