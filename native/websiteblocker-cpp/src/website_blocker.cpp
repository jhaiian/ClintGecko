#include "website_blocker.h"

#include <algorithm>
#include <cctype>
#include <cstdio>
#include <cstring>
#include <fcntl.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>

namespace websiteblocker {

namespace {

constexpr uint64_t kFnvOffsetBasis = 14695981039346656037ULL;
constexpr uint64_t kFnvPrime = 1099511628211ULL;

uint64_t Fnv1a64(const char* data, size_t len) {
    uint64_t hash = kFnvOffsetBasis;
    for (size_t i = 0; i < len; ++i) {
        hash ^= static_cast<uint8_t>(data[i]);
        hash *= kFnvPrime;
    }
    return hash;
}

bool IsValidDomainChar(unsigned char c) {
    return std::isalnum(c) || c == '.' || c == '-';
}

bool LooksLikeIp(const std::string& token) {
    if (token.empty()) return false;
    for (char c : token) {
        if (!(std::isxdigit(static_cast<unsigned char>(c)) || c == '.' || c == ':')) return false;
    }
    return token.find('.') != std::string::npos || token.find(':') != std::string::npos;
}

const char* kBoilerplate[] = {
    "0.0.0.0", "localhost", "localhost.localdomain", "local", "broadcasthost",
    "ip6-localhost", "ip6-loopback", "ip6-allnodes", "ip6-allrouters",
    "ip6-localnet", "ip6-mcastprefix"
};

}  // namespace

uint64_t HashDomain(std::string_view domain) {
    return Fnv1a64(domain.data(), domain.size());
}

std::string NormalizeDomain(const std::string& raw) {
    size_t start = 0;
    size_t end = raw.size();
    while (start < end && std::isspace(static_cast<unsigned char>(raw[start]))) start++;
    while (end > start && std::isspace(static_cast<unsigned char>(raw[end - 1]))) end--;
    while (end > start && raw[end - 1] == '.') end--;

    std::string out;
    out.reserve(end - start);
    for (size_t i = start; i < end; ++i) {
        unsigned char c = static_cast<unsigned char>(raw[i]);
        if (!IsValidDomainChar(c)) return std::string();
        out.push_back(static_cast<char>(std::tolower(c)));
    }
    return out;
}

bool IsBoilerplateHost(const std::string& domain) {
    for (const char* entry : kBoilerplate) {
        if (domain == entry) return true;
    }
    return false;
}

Builder::Stats Builder::AddText(const std::string& text) {
    Stats stats;
    size_t pos = 0;
    const size_t len = text.size();

    while (pos < len) {
        size_t lineEnd = text.find('\n', pos);
        if (lineEnd == std::string::npos) lineEnd = len;
        size_t lineStart = pos;
        size_t lineLen = lineEnd - lineStart;
        pos = lineEnd + 1;

        while (lineLen > 0 && (text[lineStart] == ' ' || text[lineStart] == '\t')) {
            lineStart++;
            lineLen--;
        }
        while (lineLen > 0 && std::isspace(static_cast<unsigned char>(text[lineStart + lineLen - 1]))) {
            lineLen--;
        }
        if (lineLen == 0) continue;

        char first = text[lineStart];
        if (first == '#' || first == ';' || first == '!') continue;

        std::string line = text.substr(lineStart, lineLen);
        size_t sep = line.find_first_of(" \t");
        std::string domainToken;
        if (sep == std::string::npos) {
            domainToken = line;
        } else {
            std::string firstToken = line.substr(0, sep);
            size_t rest = line.find_first_not_of(" \t", sep);
            std::string secondToken = (rest == std::string::npos) ? std::string()
                                                                    : line.substr(rest, line.find_first_of(" \t", rest) - rest);
            if (LooksLikeIp(firstToken) && !secondToken.empty()) {
                domainToken = secondToken;
            } else {
                domainToken = firstToken;
            }
        }

        std::string domain = NormalizeDomain(domainToken);
        if (domain.empty() || domain.find('.') == std::string::npos || IsBoilerplateHost(domain)) {
            stats.skipped++;
            continue;
        }

        hashes_.push_back(HashDomain(domain));
        stats.added++;
    }
    return stats;
}

bool Builder::Finalize(const std::string& outputPath, uint64_t* outDomainCount, uint64_t* outSizeBytes) {
    std::sort(hashes_.begin(), hashes_.end());
    hashes_.erase(std::unique(hashes_.begin(), hashes_.end()), hashes_.end());

    FILE* file = std::fopen(outputPath.c_str(), "wb");
    if (!file) return false;

    const uint64_t domainCount = hashes_.size();
    const uint64_t dataBytes = domainCount * sizeof(uint64_t);
    uint64_t checksum = Fnv1a64(reinterpret_cast<const char*>(hashes_.data()), dataBytes);

    FileHeader header{kMagic, kVersion, domainCount, checksum};

    bool ok = std::fwrite(&header, sizeof(header), 1, file) == 1;
    if (ok && domainCount > 0) {
        ok = std::fwrite(hashes_.data(), sizeof(uint64_t), domainCount, file) == domainCount;
    }
    std::fclose(file);
    if (!ok) return false;

    if (outDomainCount) *outDomainCount = domainCount;
    if (outSizeBytes) *outSizeBytes = sizeof(header) + dataBytes;
    return true;
}

Database* Database::Load(const std::string& path) {
    int fd = open(path.c_str(), O_RDONLY);
    if (fd < 0) return nullptr;

    struct stat st{};
    if (fstat(fd, &st) != 0 || st.st_size < static_cast<off_t>(sizeof(FileHeader))) {
        close(fd);
        return nullptr;
    }

    size_t size = static_cast<size_t>(st.st_size);
    void* mapping = mmap(nullptr, size, PROT_READ, MAP_PRIVATE, fd, 0);
    close(fd);
    if (mapping == MAP_FAILED) return nullptr;

    const FileHeader* header = reinterpret_cast<const FileHeader*>(mapping);
    const uint64_t expectedBytes = sizeof(FileHeader) + header->domainCount * sizeof(uint64_t);
    if (header->magic != kMagic || header->version != kVersion || expectedBytes != size) {
        munmap(mapping, size);
        return nullptr;
    }

    const uint64_t* hashes = reinterpret_cast<const uint64_t*>(
        reinterpret_cast<const uint8_t*>(mapping) + sizeof(FileHeader));
    uint64_t checksum = Fnv1a64(reinterpret_cast<const char*>(hashes), header->domainCount * sizeof(uint64_t));
    if (checksum != header->checksum) {
        munmap(mapping, size);
        return nullptr;
    }

    Database* db = new Database();
    db->mapping_ = mapping;
    db->mappingSize_ = size;
    db->hashes_ = hashes;
    db->domainCount_ = header->domainCount;
    return db;
}

Database::~Database() {
    if (mapping_) munmap(mapping_, mappingSize_);
}

bool Database::ContainsHash(uint64_t hash) const {
    if (domainCount_ == 0) return false;
    const uint64_t* begin = hashes_;
    const uint64_t* end = hashes_ + domainCount_;
    const uint64_t* it = std::lower_bound(begin, end, hash);
    return it != end && *it == hash;
}

bool Database::IsBlocked(const std::string& host) const {
    std::string normalized = NormalizeDomain(host);
    if (normalized.empty()) return false;

    std::string_view view(normalized);
    if (ContainsHash(HashDomain(view))) return true;

    size_t dot = view.find('.');
    while (dot != std::string_view::npos) {
        std::string_view suffix = view.substr(dot + 1);
        if (suffix.find('.') == std::string_view::npos) break;
        if (ContainsHash(HashDomain(suffix))) return true;
        dot = view.find('.', dot + 1);
    }
    return false;
}

}  // namespace websiteblocker
