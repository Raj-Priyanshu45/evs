import { useEffect, useRef, useState } from 'react'
import { AlertTriangle, ArrowRight, CheckCircle2, FileImage, ImagePlus, LoaderCircle, ScanSearch, Sparkles, UploadCloud, X } from 'lucide-react'
import { analyzeWasteImage, type WasteImageAnalysis } from '../lib/api'

const MAX_FILE_SIZE = 10 * 1024 * 1024

export default function ImageAnalyzer() {
  const inputRef = useRef<HTMLInputElement>(null)
  const [file, setFile] = useState<File | null>(null)
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const [result, setResult] = useState<WasteImageAnalysis | null>(null)
  const [error, setError] = useState('')
  const [dragActive, setDragActive] = useState(false)
  const [analyzing, setAnalyzing] = useState(false)

  useEffect(() => {
    if (!file) {
      setPreviewUrl(null)
      return
    }
    const objectUrl = URL.createObjectURL(file)
    setPreviewUrl(objectUrl)
    return () => URL.revokeObjectURL(objectUrl)
  }, [file])

  const selectFile = (candidate?: File) => {
    if (!candidate) return
    setFile(null)
    setError('')
    setResult(null)
    if (!['image/jpeg', 'image/png'].includes(candidate.type) && !/\.(jpe?g|png)$/i.test(candidate.name)) {
      setError('Choose a JPG, JPEG, or PNG image.')
      return
    }
    if (candidate.size > MAX_FILE_SIZE) {
      setError('This image is larger than 10 MB. Choose a smaller photo.')
      return
    }
    setFile(candidate)
  }

  const runAnalysis = async () => {
    if (!file || analyzing) return
    setAnalyzing(true)
    setError('')
    try {
      setResult(await analyzeWasteImage(file))
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'The image could not be analyzed. Please try again.')
    } finally {
      setAnalyzing(false)
    }
  }

  const reset = () => {
    setFile(null)
    setResult(null)
    setError('')
    if (inputRef.current) inputRef.current.value = ''
  }

  return (
    <section className="scan-section section-wrap" id="image-scanner">
      <div className="scan-heading">
        <div>
          <div className="micro-label"><span className="micro-line" /> THE VISUAL WASTE LAB</div>
          <h2>Give your waste a <em>closer look.</em></h2>
        </div>
        <div className="scan-heading-side"><p>Drop in a photo and let a vision model identify the material, estimate the waste category, and suggest a more responsible next step.</p><div className="scan-model-pills"><span><Sparkles size={13} /> YOLOv8n-cls · 8 classes</span><span><CheckCircle2 size={13} /> Local inference</span></div></div>
      </div>

      <div className="scan-workspace">
        <div className="scan-upload-column">
          <div
            className={`scan-dropzone ${dragActive ? 'scan-drag-active' : ''} ${previewUrl ? 'scan-has-preview' : ''}`}
            onDragOver={(event) => { event.preventDefault(); setDragActive(true) }}
            onDragLeave={() => setDragActive(false)}
            onDrop={(event) => { event.preventDefault(); setDragActive(false); selectFile(event.dataTransfer.files[0]) }}
          >
            <input ref={inputRef} className="scan-file-input" type="file" accept="image/jpeg,image/png,.jpg,.jpeg,.png" onChange={(event) => selectFile(event.target.files?.[0])} aria-label="Choose a waste image" />
            {previewUrl ? (
              <>
                <img className="scan-preview-image" src={previewUrl} alt="Selected waste item preview" />
                <div className="scan-preview-overlay"><span><FileImage size={14} /> Ready to scan</span><button onClick={reset} aria-label="Remove selected image"><X size={15} /></button></div>
              </>
            ) : (
              <button className="scan-picker" type="button" onClick={() => inputRef.current?.click()}>
                <span className="scan-upload-icon"><UploadCloud size={25} /></span>
                <strong>Drop a waste photo here</strong>
                <span>or click to browse your files</span>
                <small>JPG or PNG · up to 10 MB</small>
              </button>
            )}
          </div>
          <div className="scan-file-row">
            <span className="scan-file-info"><ImagePlus size={15} /> {file ? file.name : 'One clear item works best'}</span>
            <span>{file ? `${(file.size / (1024 * 1024)).toFixed(2)} MB` : 'Single-item photo'}</span>
          </div>
          {error && <div className="scan-error" role="alert"><AlertTriangle size={16} /> {error}</div>}
          <button className="scan-submit" onClick={runAnalysis} disabled={!file || analyzing}>
            {analyzing ? <><LoaderCircle className="scan-spinner" size={17} /> Reading image…</> : <><ScanSearch size={17} /> Analyze this item <ArrowRight size={16} /></>}
          </button>
          <p className="scan-privacy"><span /> Your image is saved on the backend server in the local uploads folder.</p>
        </div>

        <div className="scan-result-column" aria-live="polite">
          {analyzing ? (
            <div className="scan-result-empty scan-processing"><span className="scan-processing-orbit"><ScanSearch size={28} /></span><span className="scan-result-kicker">VISION PIPELINE RUNNING</span><h3>Looking at the material.</h3><p>The image is being validated, stored, and processed by the local vision model.</p><div className="scan-progress"><i /></div></div>
          ) : result ? (
            <div className="scan-result-content">
              <div className="scan-result-topline"><span className="scan-result-kicker">{result.analyzed ? 'AI CLASSIFICATION RESULT' : 'IMAGE STORED'}</span><span className={`scan-result-status ${result.analyzed ? 'status-success' : 'status-pending'}`}>{result.analyzed ? <><CheckCircle2 size={13} /> Analyzed</> : <><AlertTriangle size={13} /> Needs setup</>}</span></div>
              {result.analyzed ? (
                <>
                  <div className="scan-prediction-head"><div><span className="scan-muted-label">PREDICTED ITEM TYPE</span><h3>{result.predictedLabel?.replace(/[-_]/g, ' ')}</h3><p>{result.category} · {result.recyclability}</p></div><div className="scan-confidence"><strong>{Math.round((result.confidence ?? 0) * 100)}<small>%</small></strong><span>confidence</span></div></div>
                  <div className="scan-advice-box"><span className="scan-advice-icon"><Sparkles size={18} /></span><div><span className="scan-muted-label">RECOMMENDED NEXT STEP</span><strong>{result.disposalType}</strong><p>{result.instructions[0]}</p></div></div>
                  <div className="scan-details-row"><div><span className="scan-muted-label">MATERIAL CATEGORY</span><strong>{result.category}</strong></div><div><span className="scan-muted-label">IMAGE DIMENSIONS</span><strong>{result.imageWidth} × {result.imageHeight}</strong></div></div>
                  {result.topPredictions.length > 1 && <div className="scan-alternatives"><span className="scan-muted-label">OTHER MODEL GUESSES</span>{result.topPredictions.slice(0, 4).map((prediction) => <div className="scan-score-row" key={prediction.label}><span>{prediction.label.replace(/[-_]/g, ' ')}</span><div><i style={{ width: `${Math.max(2, prediction.score * 100)}%` }} /></div><strong>{Math.round(prediction.score * 100)}%</strong></div>)}</div>}
                  {result.avoid.length > 0 && <div className="scan-caution"><AlertTriangle size={15} /><span><strong>What to avoid:</strong> {result.avoid[0]}</span></div>}
                </>
              ) : (
                <div className="scan-result-empty scan-config-message"><span className="scan-processing-orbit"><Sparkles size={26} /></span><h3>Your photo is saved.</h3><p>{result.message}</p><div className="scan-config-steps"><span>01 <code>HF_VISION_ENABLED=true</code> — enable model inference</span><span>02 First scan downloads the model from Hugging Face</span><span>03 A token is optional for this public model</span></div></div>
              )}
              <div className="scan-result-foot"><span><CheckCircle2 size={13} /> Stored successfully</span><a href={result.imageUrl} target="_blank" rel="noreferrer">Open stored image <ArrowRight size={13} /></a></div>
              <p className="scan-result-message">{result.message}</p>
            </div>
          ) : (
            <div className="scan-result-empty"><div className="scan-empty-art"><span className="scan-art-frame"><FileImage size={37} /></span><span className="scan-art-spark spark-a">✳</span><span className="scan-art-spark spark-b">✦</span><span className="scan-art-line" /></div><span className="scan-result-kicker">YOUR RESULT WILL APPEAR HERE</span><h3>One photo. A smarter next step.</h3><p>The model returns a predicted waste label with confidence scores, then the app adds category-specific disposal guidance.</p><div className="scan-result-mini-tags"><span>Material label</span><span>Confidence</span><span>Disposal advice</span></div></div>
          )}
        </div>
      </div>
      <p className="scan-disclaimer">AI predictions can be wrong, especially for mixed or cluttered images. This prototype classifies the main material category; it is not a safety certification. Always follow local collection rules for batteries, chemicals, and e-waste.</p>
    </section>
  )
}
