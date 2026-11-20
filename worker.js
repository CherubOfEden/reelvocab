const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type",
};

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: CORS_HEADERS });
    }
    if (request.method !== "POST") {
      return jsonResponse({ error: "Method not allowed" }, 405);
    }

    let body;
    try {
      body = await request.json();
    } catch {
      return jsonResponse({ error: "Invalid JSON body" }, 400);
    }

    const query = typeof body?.query === "string" ? body.query.trim() : "";
    const type = typeof body?.type === "string" ? body.type.trim() : "";
    if (!query) {
      return jsonResponse({ error: "Missing query" }, 400);
    }
    if (type !== "definition") {
      return jsonResponse({ error: "Unsupported type" }, 400);
    }
    if (!env?.GEMINI_API_KEY) {
      return jsonResponse({ error: "Server is not configured" }, 500);
    }

    const prompt = [
      "Define the exact English word or multi-word phrase below.",
      "Treat idioms, slang, and phrasal verbs as single expressions.",
      "Never split or truncate the query.",
      "",
      `Query: ${JSON.stringify(query)}`,
      "",
      "Return only a JSON object with these keys:",
      "- definition: a concise learner-friendly meaning in 1-2 sentences",
      "- examples: exactly 2 natural sentences that use the query exactly",
      "- synonyms: up to 8 single words or short phrases",
      "- antonyms: up to 8 single words or short phrases",
    ].join("\n");

    let geminiResponse;
    try {
      geminiResponse = await fetch(
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "x-goog-api-key": env.GEMINI_API_KEY,
          },
          body: JSON.stringify({
            contents: [{ role: "user", parts: [{ text: prompt }] }],
            generationConfig: {
              temperature: 0.2,
              maxOutputTokens: 512,
              responseMimeType: "application/json",
            },
          }),
        },
      );
    } catch {
      return jsonResponse({ error: "AI service is unavailable" }, 502);
    }

    if (!geminiResponse.ok) {
      return jsonResponse({ error: "AI service returned an error" }, 502);
    }

    let geminiBody;
    try {
      geminiBody = await geminiResponse.json();
    } catch {
      return jsonResponse({ error: "AI service returned invalid data" }, 502);
    }

    const text = (geminiBody?.candidates?.[0]?.content?.parts ?? [])
      .map((part) => (typeof part?.text === "string" ? part.text : ""))
      .join("")
      .trim();
    if (!text) {
      return jsonResponse({ error: "AI service returned no definition" }, 502);
    }

    let parsed;
    try {
      parsed = parseJsonObject(text);
    } catch {
      return jsonResponse({ error: "AI service returned malformed JSON" }, 502);
    }

    const definition = normalizeString(parsed.definition ?? parsed.meaning);
    if (!definition) {
      return jsonResponse({ error: "AI service returned no definition" }, 502);
    }

    const examples = normalizeList(parsed.examples).slice(0, 2);
    if (examples.length === 0) {
      const singleExample = normalizeString(parsed.example);
      if (singleExample) examples.push(singleExample);
    }

    return jsonResponse(
      {
        query,
        type: "definition",
        definition,
        examples,
        synonyms: normalizeList(parsed.synonyms).slice(0, 8),
        antonyms: normalizeList(parsed.antonyms).slice(0, 8),
      },
      200,
    );
  },
};

function jsonResponse(body, status) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      ...CORS_HEADERS,
      "Content-Type": "application/json; charset=utf-8",
    },
  });
}

function normalizeString(value) {
  return typeof value === "string" ? value.trim() : "";
}

function normalizeList(value) {
  const items = Array.isArray(value)
    ? value
    : typeof value === "string"
      ? value.split(",")
      : [];
  return items
    .map((item) => normalizeString(item))
    .filter((item) => item.length > 0);
}

function parseJsonObject(text) {
  const start = text.indexOf("{");
  const end = text.lastIndexOf("}");
  if (start < 0 || end <= start) {
    throw new Error("No JSON object found");
  }
  return JSON.parse(text.slice(start, end + 1));
}
