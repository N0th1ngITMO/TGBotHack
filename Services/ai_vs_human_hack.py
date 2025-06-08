import os
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import numpy
from contextlib import asynccontextmanager
from transformers import BertTokenizer, BertForSequenceClassification
import torch

# Определяем lifespan-контекст, где инициализируем и закрываем пул
@asynccontextmanager
async def lifespan(app: FastAPI):
    yield

# Передаём lifespan при создании FastAPI
app = FastAPI(lifespan=lifespan)
model_name = "annoyingrusk/rubert-ai-vs-human-classifier"
ai_vs_human_tokenizer = BertTokenizer.from_pretrained(model_name)
ai_vs_human_model = BertForSequenceClassification.from_pretrained(model_name)

class TextRequest(BaseModel):
    text: str

class AnswerResponse(BaseModel):
    is_human: int

@app.post("/check_ai", response_model=AnswerResponse)
async def read_item(request: TextRequest):
    inputs = ai_vs_human_tokenizer(request.text, padding=True, truncation=True, max_length=128, return_tensors="pt")
    with torch.no_grad():
        outputs = ai_vs_human_model(**inputs)
        logits = outputs.logits
    return {"is_human":0 if logits[0][0] > 0.5 else 1}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "ai_vs_human:app", 
        host="0.0.0.0", 
        port=int(os.getenv("PORT", 8000)), 
        reload=True
    )
