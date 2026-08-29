# NeuNest

An offline, on-device AI assistant for Android using Retrieval-Augmented Generation (RAG). Private, low-latency, and works without internet or cloud APIs.

<img width="300" alt="ffcdbc20-0746-45a8-8680-45c14731fa78" src="https://github.com/user-attachments/assets/adb53ef8-57a4-4bc4-95b0-0ff74738adb4" />

## Why NeuNest
- Fully offline and privacy-first — no data leaves your device  
- Fast, local responses with no API costs  
- Works in low/ no-connectivity environments (flights, rural areas)

## Key Features
- Import PDFs, TXT, MD and index them locally  
- Lightweight on-device embeddings + vector store  
- RAG: retrieve relevant chunks and generate answers locally  
- Configurable model selection and hardware acceleration (NNAPI/GPU)

## How it works (brief)
1. Ingest → extract text and chunk documents.  
2. Embed → create local vector embeddings.  
3. Retrieve → find top-k relevant chunks.  
4. Generate → use on-device model to answer with citations.

## Quick start
1. Clone: git clone https://github.com/abhishekumar2x/NeuNest.git  
2. Open in Android Studio.  
3. Download/prepare quantized on-device models (place per docs).  
4. Run on a physical device (recommended).

## Notes & tips
- Use quantized/smaller models for low-RAM devices.  
- Enable NNAPI/GPU if supported.  
- Cache embeddings to avoid reprocessing.
