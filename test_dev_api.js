const http = require('http');

http.get('http://localhost:7001/api/dev/registry', (resp) => {
  let data = '';
  resp.on('data', chunk => data += chunk);
  resp.on('end', () => {
    try {
      const parsed = JSON.parse(data);
      if (parsed.length > 0) {
        const id = parsed[0].id;
        http.get('http://localhost:7001/api/dev/registry/' + id + '/detail', (r2) => {
          let d2 = '';
          r2.on('data', chunk => d2 += chunk);
          r2.on('end', () => console.log('Detail:', d2));
        });
      } else {
        console.log("No registries found");
      }
    } catch(e) {
      console.log("Error parsing:", data);
    }
  });
}).on("error", (err) => console.log("Error: " + err.message));
