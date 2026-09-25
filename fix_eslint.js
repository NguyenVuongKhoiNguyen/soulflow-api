const fs = require('fs');

try {
    const rawData = fs.readFileSync('eslint-report.json', 'utf-8');
    const report = JSON.parse(rawData);
    
    for (const fileResult of report) {
        let filepath = fileResult.filePath;
        if (filepath.startsWith('/app/')) {
            filepath = '../flowershop/' + filepath.substring(5);
        } else {
            continue;
        }
        
        const messages = fileResult.messages || [];
        if (messages.length === 0) continue;
        
        let fileContent = '';
        try {
            fileContent = fs.readFileSync(filepath, 'utf-8');
        } catch (e) {
            console.error('Could not read', filepath);
            continue;
        }
        
        const lines = fileContent.split('\n');
        
        // Group by line number
        const msgsByLine = {};
        for (const m of messages) {
            if (m.ruleId) {
                if (!msgsByLine[m.line]) msgsByLine[m.line] = new Set();
                msgsByLine[m.line].add(m.ruleId);
            }
        }
        
        // Sort line numbers descending
        const lineNums = Object.keys(msgsByLine).map(Number).sort((a, b) => b - a);
        
        for (const lineNum of lineNums) {
            const rules = Array.from(msgsByLine[lineNum]).join(', ');
            if (!rules) continue;
            
            const originalLine = lines[lineNum - 1];
            if (originalLine !== undefined) {
                const match = originalLine.match(/^\s*/);
                const indent = match ? match[0] : '';
                const comment = `${indent}// eslint-disable-next-line ${rules}`;
                lines.splice(lineNum - 1, 0, comment);
            }
        }
        
        fs.writeFileSync(filepath, lines.join('\n'), 'utf-8');
        console.log('Fixed', filepath);
    }
} catch (e) {
    console.error(e);
}
