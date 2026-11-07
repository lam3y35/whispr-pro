namespace Murmur.Speech;

/// <summary>
/// Encodes raw 16 kHz float PCM audio into standard 16-bit PCM WAV byte arrays in-memory.
/// </summary>
public static class WavEncoder
{
    /// <summary>
    /// Converts a span of float samples [-1.0, 1.0] at <paramref name="sampleRate"/> into a 16-bit WAV file.
    /// </summary>
    public static byte[] Encode16BitPcm(ReadOnlySpan<float> samples, int sampleRate)
    {
        var dataSize = samples.Length * 2; // 16 bits = 2 bytes per sample
        var wav = new byte[44 + dataSize];

        // "RIFF"
        wav[0] = 0x52; wav[1] = 0x49; wav[2] = 0x46; wav[3] = 0x46;
        var fileSizeMinus8 = 36 + dataSize;
        wav[4] = (byte)(fileSizeMinus8 & 0xFF);
        wav[5] = (byte)((fileSizeMinus8 >> 8) & 0xFF);
        wav[6] = (byte)((fileSizeMinus8 >> 16) & 0xFF);
        wav[7] = (byte)((fileSizeMinus8 >> 24) & 0xFF);

        // "WAVE"
        wav[8] = 0x57; wav[9] = 0x41; wav[10] = 0x56; wav[11] = 0x45;

        // "fmt "
        wav[12] = 0x66; wav[13] = 0x6D; wav[14] = 0x74; wav[15] = 0x20;
        wav[16] = 16; wav[17] = 0; wav[18] = 0; wav[19] = 0; // SubChunk1Size = 16
        wav[20] = 1; wav[21] = 0; // AudioFormat = 1 (PCM)
        wav[22] = 1; wav[23] = 0; // NumChannels = 1 (mono)

        // SampleRate
        wav[24] = (byte)(sampleRate & 0xFF);
        wav[25] = (byte)((sampleRate >> 8) & 0xFF);
        wav[26] = (byte)((sampleRate >> 16) & 0xFF);
        wav[27] = (byte)((sampleRate >> 24) & 0xFF);

        // ByteRate = SampleRate * 2 (16-bit mono)
        var byteRate = sampleRate * 2;
        wav[28] = (byte)(byteRate & 0xFF);
        wav[29] = (byte)((byteRate >> 8) & 0xFF);
        wav[30] = (byte)((byteRate >> 16) & 0xFF);
        wav[31] = (byte)((byteRate >> 24) & 0xFF);

        wav[32] = 2; wav[33] = 0; // BlockAlign = 2
        wav[34] = 16; wav[35] = 0; // BitsPerSample = 16

        // "data"
        wav[36] = 0x64; wav[37] = 0x61; wav[38] = 0x74; wav[39] = 0x61;
        wav[40] = (byte)(dataSize & 0xFF);
        wav[41] = (byte)((dataSize >> 8) & 0xFF);
        wav[42] = (byte)((dataSize >> 16) & 0xFF);
        wav[43] = (byte)((dataSize >> 24) & 0xFF);

        // PCM samples
        var offset = 44;
        for (var i = 0; i < samples.Length; i++)
        {
            var clamped = Math.Clamp(samples[i], -1.0f, 1.0f);
            var sample16 = (short)(clamped * 32767.0f);
            wav[offset++] = (byte)(sample16 & 0xFF);
            wav[offset++] = (byte)((sample16 >> 8) & 0xFF);
        }

        return wav;
    }
}
