using System.Net.Http.Headers;
using System.Text.Json;
using Murmur.Abstractions;

namespace Murmur.Speech;

/// <summary>
/// Cloud speech transcriber using Groq Cloud API (Whisper Large-v3).
/// Ultra-fast (~1-2 seconds), zero local GPU or model download required, with free renewing usage limits.
/// Supports code-switching (bilingual Arabic and English in the same utterance).
/// </summary>
public sealed class GroqWhisperTranscriber : ITranscriber
{
    private static readonly HttpClient s_httpClient = new()
    {
        Timeout = TimeSpan.FromSeconds(30),
    };

    private readonly string _apiKey;
    private readonly string _model;
    private readonly string _prompt;

    /// <summary>
    /// Initializes a new instance of the Groq Whisper transcriber.
    /// </summary>
    /// <param name="apiKey">Groq Cloud API key.</param>
    /// <param name="model">Whisper model name (defaults to "whisper-large-v3").</param>
    /// <param name="prompt">Prompt conditioning for code-switching.</param>
    public GroqWhisperTranscriber(
        string apiKey,
        string model = "whisper-large-v3",
        string? prompt = null)
    {
        _apiKey = apiKey.Trim();
        _model = string.IsNullOrWhiteSpace(model) ? "whisper-large-v3" : model.Trim();
        _prompt = prompt ?? "Hello, مرحبا, English and عربي code-switching dictation.";
    }

    /// <inheritdoc />
    public bool IsReady => !string.IsNullOrWhiteSpace(_apiKey);

    /// <inheritdoc />
    public ValueTask<bool> LoadAsync(CancellationToken cancellationToken) =>
        ValueTask.FromResult(IsReady);

    /// <inheritdoc />
    public async ValueTask<string> TranscribeAsync(
        ReadOnlyMemory<float> samples,
        IReadOnlyList<string> biasPhrases,
        CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(_apiKey) || samples.Length == 0)
            return string.Empty;

        cancellationToken.ThrowIfCancellationRequested();

        // Convert 16 kHz float samples to 16-bit PCM WAV in memory
        var wavBytes = WavEncoder.Encode16BitPcm(samples.Span, AudioChunk.SampleRate);

        using var content = new MultipartFormDataContent();

        using var fileContent = new ByteArrayContent(wavBytes);
        fileContent.Headers.ContentType = new MediaTypeHeaderValue("audio/wav");
        content.Add(fileContent, "file", "audio.wav");

        content.Add(new StringContent(_model), "model");
        content.Add(new StringContent("json"), "response_format");
        content.Add(new StringContent("0"), "temperature");

        var effectivePrompt = _prompt;
        if (biasPhrases is { Count: > 0 })
        {
            effectivePrompt += ", " + string.Join(", ", biasPhrases);
        }
        content.Add(new StringContent(effectivePrompt), "prompt");

        using var request = new HttpRequestMessage(HttpMethod.Post, "https://api.groq.com/openai/v1/audio/transcriptions")
        {
            Content = content,
        };
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", _apiKey);

        using var response = await s_httpClient.SendAsync(request, cancellationToken).ConfigureAwait(false);
        var json = await response.Content.ReadAsStringAsync(cancellationToken).ConfigureAwait(false);

        if (!response.IsSuccessStatusCode)
        {
            throw new HttpRequestException($"Groq API error ({response.StatusCode}): {json}");
        }

        using var doc = JsonDocument.Parse(json);
        if (doc.RootElement.TryGetProperty("text", out var textProp))
        {
            return textProp.GetString()?.Trim() ?? string.Empty;
        }

        return string.Empty;
    }

    /// <inheritdoc />
    public ValueTask DisposeAsync() => ValueTask.CompletedTask;
}
