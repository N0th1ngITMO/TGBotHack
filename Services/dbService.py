import os
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import asyncpg
import numpy
from contextlib import asynccontextmanager

# Определяем lifespan-контекст, где инициализируем и закрываем пул
@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.db = await asyncpg.create_pool(
        user="testuser",
        password="testpass",
        database="testdb",
        host="localhost",
        port="5432",
    )
    yield
    await app.state.db.close()

# Передаём lifespan при создании FastAPI
app = FastAPI(lifespan=lifespan)

class Item(BaseModel):
    id: int

@app.get("/items/{item_id}", response_model=Item)
async def read_item(item_id: int):
    print(item_id)
    row = await app.state.db.fetchrow(
        "SELECT id FROM items WHERE id=$1",
        item_id
    )
    
    return {"id": 1 if row else 0}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "testCode:app", 
        host="0.0.0.0", 
        port=int(os.getenv("PORT", 8000)), 
        reload=True
    )
