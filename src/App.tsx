import { useCallback, useEffect, useMemo, useState } from 'react'
import { ArrowDownRight, ArrowRight, Check, ChevronDown, CircleHelp, Leaf, Search, ShieldCheck, Sparkles, Sprout, Recycle, Menu, X } from 'lucide-react'
import WasteCard from './components/WasteCard'
import ItemDetails from './components/ItemDetails'
import ImageAnalyzer from './components/ImageAnalyzer'
import { loadCatalog } from './lib/api'
import type { WasteCategory, WasteItem } from './data/wasteItems'

const categoryIcons: Record<string, string> = {
  all: '◈', recyclables: '♻', organic: '✳', 'e-waste': '⌁', hazardous: '!', textiles: '↻',
}

export default function App() {
  const [items, setItems] = useState<WasteItem[]>([])
  const [categories, setCategories] = useState<WasteCategory[]>([])
  const [query, setQuery] = useState('')
  const [category, setCategory] = useState('all')
  const [activeItem, setActiveItem] = useState<WasteItem | null>(null)
  const [apiConnected, setApiConnected] = useState(false)
  const [loading, setLoading] = useState(true)
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)

  useEffect(() => {
    let alive = true
    loadCatalog().then((catalog) => {
      if (!alive) return
      setItems(catalog.items)
      setCategories(catalog.categories)
      setApiConnected(catalog.connected)
      setLoading(false)
    })
    return () => { alive = false }
  }, [])

  const filteredItems = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase()
    return items.filter((item) => {
      const categoryMatches = category === 'all' || item.categorySlug === category
      const queryMatches = !normalizedQuery || [item.name, item.description, item.category, item.recyclability, item.disposalType]
        .some((value) => value.toLowerCase().includes(normalizedQuery))
      return categoryMatches && queryMatches
    })
  }, [items, query, category])

  const openDetails = useCallback((item: WasteItem) => setActiveItem(item), [])
  const closeDetails = useCallback(() => setActiveItem(null), [])

  const startSearching = () => {
    document.getElementById('catalog')?.scrollIntoView({ behavior: 'smooth' })
    window.setTimeout(() => document.getElementById('waste-search')?.focus(), 250)
  }

  return (
    <div className="app-shell">
      <header className="site-header">
        <a className="brand" href="#top" aria-label="WasteWise home">
          <span className="brand-mark"><Sprout size={21} strokeWidth={2.2} /></span>
          <span>Waste<span className="brand-strong">Wise</span><small>GOOD HABITS, LESS WASTE</small></span>
        </a>
        <nav className={`main-nav ${mobileMenuOpen ? 'nav-open' : ''}`} aria-label="Main navigation">
          <a href="#image-scanner" onClick={() => setMobileMenuOpen(false)}>AI image scan</a>
          <a href="#catalog" onClick={() => setMobileMenuOpen(false)}>Explore waste</a>
          <a href="#how-it-works" onClick={() => setMobileMenuOpen(false)}>How it works</a>
          <a href="#quick-rules" onClick={() => setMobileMenuOpen(false)}>Quick rules</a>
        </nav>
        <div className="header-right">
          <span className="status-dot"><i /> {apiConnected ? 'Live catalog' : 'Demo mode'}</span>
          <button className="header-cta" onClick={startSearching}>Find my bin <ArrowRight size={15} /></button>
        </div>
        <button className="mobile-menu" onClick={() => setMobileMenuOpen((value) => !value)} aria-label={mobileMenuOpen ? 'Close navigation' : 'Open navigation'}>
          {mobileMenuOpen ? <X size={23} /> : <Menu size={23} />}
        </button>
      </header>

      <main id="top">
        <section className="hero">
          <div className="hero-copy">
            <div className="micro-label"><span className="micro-line" /> A SMALL STEP, A BIGGER IMPACT</div>
            <h1>Waste less.<br /><em>Sort right.</em></h1>
            <p className="hero-intro">Not sure which bin it belongs in? Get a clear answer for everyday waste, plus the next step to dispose of it responsibly.</p>
            <div className="hero-actions">
              <button className="primary-button" onClick={startSearching}>Find an item <ArrowRight size={17} /></button>
              <a href="#how-it-works" className="text-link">How it works <ArrowDownRight size={16} /></a>
            </div>
            <div className="hero-proof"><div className="proof-avatars"><span>♻</span><span>✳</span><span>↻</span></div><p><strong>Small choices add up.</strong><br />Make the next one a better one.</p></div>
          </div>
          <div className="hero-visual" aria-label="Illustration of waste items being sorted into the correct bins" role="img">
            <div className="visual-sun" />
            <div className="visual-stamp"><span>LESS<br />WASTE</span><Recycle size={22} /></div>
            <div className="visual-label label-one"><span className="label-icon">↗</span><span><strong>Know the bin</strong><small>Before you toss</small></span></div>
            <div className="visual-label label-two"><span className="label-icon leaf-label"><Leaf size={17} /></span><span><strong>Keep it circular</strong><small>Reuse. Repair. Recycle.</small></span></div>
            <div className="illustration-ground" />
            <div className="plant plant-left"><i /><i /><i /><b /></div>
            <div className="bin bin-blue"><div className="bin-lid" /><div className="bin-symbol">↻</div><span>RECYCLE</span></div>
            <div className="bin bin-green"><div className="bin-lid" /><div className="bin-symbol">✳</div><span>ORGANIC</span></div>
            <div className="floating-item bottle">🧴</div>
            <div className="floating-item carton">📦</div>
            <div className="floating-item apple">🍎</div>
            <div className="floating-item can">🥫</div>
            <div className="sprout-ornament">✳</div>
            <div className="visual-caption"><span>01 / 03</span><div /><span>EVERYDAY SORTING</span></div>
          </div>
          <div className="hero-bottomline"><span><span className="bottomline-star">✳</span> LESS LANDFILL, MORE POSSIBILITY</span><span>MADE FOR EVERYDAY LIFE <span className="bottomline-arrow">↘</span></span></div>
        </section>

        <ImageAnalyzer />

        <section className="catalog-section section-wrap" id="catalog">
          <div className="section-heading">
            <div><div className="micro-label"><span className="micro-line" /> YOUR SORTING COMPANION</div><h2>Where does it <em>go?</em></h2></div>
            <p>Search an item or browse by category. We’ll help you find a sensible next step.</p>
          </div>
          <div className="search-row">
            <label className="search-box" htmlFor="waste-search"><Search size={20} /><input id="waste-search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Try ‘battery’, ‘bottle’, ‘food scraps’…" /><kbd>/</kbd>{query && <button className="clear-search" onClick={() => setQuery('')} aria-label="Clear search"><X size={16} /></button>}</label>
            <div className="result-count"><strong>{filteredItems.length.toString().padStart(2, '0')}</strong> items to explore <ChevronDown size={15} /></div>
          </div>
          <div className="filter-row" role="group" aria-label="Filter items by category">
            {categories.map((cat) => <button key={cat.id} className={`filter-chip ${category === cat.id ? 'filter-active' : ''}`} onClick={() => setCategory(cat.id)}><span>{categoryIcons[cat.id] ?? '•'}</span>{cat.label}{cat.id === 'all' && <small>{items.length}</small>}</button>)}
          </div>
          {!apiConnected && <div className="demo-notice"><span className="notice-dot" /> Showing built-in sample data because the Spring Boot API is not connected. Start the backend to switch to the live catalog.</div>}
          <div className="catalog-grid">
            {loading ? <div className="empty-state"><span className="loading-ring" /><h3>Getting the guide ready…</h3><p>Loading the sample waste catalog.</p></div> : filteredItems.map((item) => <WasteCard key={item.id} item={item} onOpen={openDetails} />)}
            {!loading && filteredItems.length === 0 && <div className="empty-state"><span className="empty-search-icon"><Search size={23} /></span><h3>No matches this time.</h3><p>Try a broader search or choose “All items” to explore the guide.</p><button className="reset-button" onClick={() => { setQuery(''); setCategory('all') }}>Reset filters <ArrowRight size={15} /></button></div>}
          </div>
          <div className="catalog-bottom"><span>Showing a starter collection of {items.length} everyday items</span><span>More local waste rules can be added later <ArrowRight size={14} /></span></div>
        </section>

        <section className="how-section" id="how-it-works">
          <div className="section-wrap how-inner">
            <div className="how-heading"><div className="micro-label"><span className="micro-line" /> SIMPLE BY DESIGN</div><h2>Three steps to<br /><em>sort smarter.</em></h2><p>No guesswork. Just straightforward guidance when you need it.</p><a className="text-link dark-link" href="#catalog">Start with an item <ArrowRight size={16} /></a></div>
            <div className="steps-list">
              <article className="step-card"><span className="step-number">01</span><div className="step-icon"><Search size={21} /></div><div><h3>Find your item</h3><p>Search by everyday name or browse one of the waste categories.</p></div><span className="step-check"><Check size={16} /></span></article>
              <article className="step-card"><span className="step-number">02</span><div className="step-icon"><CircleHelp size={21} /></div><div><h3>Understand the category</h3><p>See whether it belongs with recycling, organics, e-waste, or special handling.</p></div><span className="step-check"><Check size={16} /></span></article>
              <article className="step-card"><span className="step-number">03</span><div className="step-icon"><ShieldCheck size={21} /></div><div><h3>Take the right next step</h3><p>Follow practical instructions and avoid common disposal mistakes.</p></div><span className="step-check"><Check size={16} /></span></article>
            </div>
          </div>
        </section>

        <section className="quick-section section-wrap" id="quick-rules">
          <div className="quick-header"><div><div className="micro-label"><span className="micro-line" /> THE LITTLE THINGS MATTER</div><h2>Keep these <em>in mind.</em></h2></div><span className="quick-count">FIELD NOTES / 03</span></div>
          <div className="quick-grid">
            <article className="quick-card"><div className="quick-card-icon icon-green"><Recycle size={20} /></div><span className="quick-number">RULE 01</span><h3>Clean before recycling</h3><p>Empty containers and keep paper/cardboard dry. Food residue can contaminate a whole batch.</p></article>
            <article className="quick-card"><div className="quick-card-icon icon-amber"><ShieldCheck size={20} /></div><span className="quick-number">RULE 02</span><h3>Separate the risky stuff</h3><p>Batteries, medicines, and certain bulbs need special handling—not the regular bin.</p></article>
            <article className="quick-card"><div className="quick-card-icon icon-lime"><Sparkles size={20} /></div><span className="quick-number">RULE 03</span><h3>Reuse comes first</h3><p>Before recycling, ask if the item can be reused, repaired, donated, or refilled.</p></article>
          </div>
        </section>

        <section className="closing-cta section-wrap">
          <div className="closing-mark"><Sprout size={27} /></div><div><span className="closing-kicker">A BETTER HABIT STARTS HERE</span><h2>One item at a time.</h2><p>Find the right next step for something you’re about to throw away.</p></div><button onClick={startSearching}>Look up an item <ArrowRight size={17} /></button><span className="closing-decoration">✳</span>
        </section>
      </main>

      <footer className="site-footer"><a className="footer-brand" href="#top"><Sprout size={18} /> WasteWise</a><span>Built for a lighter footprint, one decision at a time.</span><span>PROTOTYPE · {new Date().getFullYear()}</span></footer>
      {activeItem && <ItemDetails item={activeItem} onClose={closeDetails} />}
    </div>
  )
}
