export function buildDatedFilename(parts, extension, date = new Date()) {
  const safeParts = parts
    .filter(part => part !== undefined && part !== null && String(part).trim() !== '')
    .map(part => sanitizeFilenamePart(part))
    .filter(Boolean)

  safeParts.push(formatDate(date))
  return `${safeParts.join('-')}.${extension.replace(/^\./, '')}`
}

export function buildClassFilename(classId, type, extension) {
  return buildDatedFilename([`教学班级${classId}`, type], extension)
}

export function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob instanceof Blob ? blob : new Blob([blob]))
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  URL.revokeObjectURL(url)
}

export function sanitizeFilenamePart(value) {
  return String(value)
    .trim()
    .replace(/[\\/:*?"<>|]/g, '')
    .replace(/\s+/g, '')
    .replace(/-+/g, '-')
}

function formatDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}${month}${day}`
}
